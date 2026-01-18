package com.autou.emailai.infrastructure.ai;

import com.autou.emailai.application.ports.out.AiClient;
import com.autou.emailai.application.ports.out.dto.AiAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient implements AiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiClient.class);
    private static final String MSG_NO_API_KEY = "Chave de IA nao configurada.";
    private static final String MSG_AI_FAILURE = "Falha ao consultar a IA. Tente novamente.";
    private static final String MSG_AI_INVALID = "Resposta da IA invalida. Tente novamente.";
    private static final String PROMPT_SYSTEM = """
            Voce e um classificador de emails.
            Classifique o email como PRODUTIVO ou IMPRODUTIVO.
            Retorne JSON valido com:
            category, confidence (0 a 1), reason (curto), suggestedReply (PT-BR).
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String apiKey;

    public OpenAiClient(
            RestClient openAiRestClient,
            ObjectMapper objectMapper,
            @Value("${openai.model}") String model,
            @Value("${openai.api-key:}") String apiKey
    ) {
        this.restClient = openAiRestClient;
        this.objectMapper = objectMapper;
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public AiAnalysisResponse analyze(String cleanedEmailText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(MSG_NO_API_KEY);
        }

        Map<String, Object> payload = buildRequest(cleanedEmailText);

        try {
            String responseBody = restClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            String outputText = extractOutputText(responseBody);
            if (outputText == null || outputText.isBlank()) {
                throw new IllegalStateException(MSG_AI_INVALID);
            }

            AiAnalysisResponse response = objectMapper.readValue(outputText, AiAnalysisResponse.class);
            validateResponse(response);
            return response;
        } catch (RestClientResponseException ex) {
            LOGGER.warn("OpenAI error status={} bodyLength={}", ex.getRawStatusCode(), safeLength(ex.getResponseBodyAsString()), ex);
            throw new IllegalStateException(MSG_AI_FAILURE, ex);
        } catch (RestClientException ex) {
            LOGGER.warn("OpenAI request failed", ex);
            throw new IllegalStateException(MSG_AI_FAILURE, ex);
        } catch (IOException ex) {
            LOGGER.warn("OpenAI response parse failed", ex);
            throw new IllegalStateException(MSG_AI_INVALID, ex);
        }
    }

    private Map<String, Object> buildRequest(String cleanedEmailText) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", Map.of(
                "category", Map.of("type", "string", "enum", List.of("PRODUTIVO", "IMPRODUTIVO")),
                "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1),
                "reason", Map.of("type", "string"),
                "suggestedReply", Map.of("type", "string")
        ));
        schema.put("required", List.of("category", "confidence", "reason", "suggestedReply"));

        Map<String, Object> textFormat = Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "strict", true,
                        "name", "email_analysis",
                        "schema", schema
                )
        );

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", List.of(Map.of("type", "input_text", "text", PROMPT_SYSTEM))
        );
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(Map.of("type", "input_text", "text", cleanedEmailText))
        );

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("input", List.of(systemMessage, userMessage));
        request.put("text", textFormat);
        request.put("temperature", 0.2);
        return request;
    }

    private String extractOutputText(String responseBody) throws IOException {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode outputText = root.get("output_text");
        if (outputText != null) {
            if (outputText.isTextual()) {
                return outputText.asText();
            }
            if (outputText.isArray() && outputText.size() > 0) {
                return outputText.get(0).asText();
            }
        }

        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.get("content");
                if (content != null && content.isArray()) {
                    for (JsonNode contentItem : content) {
                        JsonNode text = contentItem.get("text");
                        if (text != null && text.isTextual()) {
                            return text.asText();
                        }
                    }
                }
            }
        }

        return null;
    }

    private void validateResponse(AiAnalysisResponse response) {
        if (response == null) {
            throw new IllegalStateException(MSG_AI_INVALID);
        }
        if (response.category() == null || response.category().isBlank()) {
            throw new IllegalStateException(MSG_AI_INVALID);
        }
        if (response.reason() == null || response.reason().isBlank()) {
            throw new IllegalStateException(MSG_AI_INVALID);
        }
        if (response.suggestedReply() == null || response.suggestedReply().isBlank()) {
            throw new IllegalStateException(MSG_AI_INVALID);
        }
    }

    private int safeLength(String value) {
        return (value == null) ? 0 : value.length();
    }
}
