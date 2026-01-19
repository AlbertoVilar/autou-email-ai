package com.autou.emailai.application;

import com.autou.emailai.application.ports.in.EmailAnalysisUseCase;
import com.autou.emailai.application.ports.out.AiClient;
import com.autou.emailai.application.ports.out.FileTextExtractor;
import com.autou.emailai.application.ports.out.dto.AiAnalysisResponse;
import com.autou.emailai.application.exception.AiNotConfiguredException;
import com.autou.emailai.application.exception.AiQuotaException;
import com.autou.emailai.application.exception.AiRequestFailedException;
import com.autou.emailai.application.exception.InvalidAiResponseException;
import com.autou.emailai.application.exception.InvalidFileException;
import com.autou.emailai.domain.EmailAnalysisResult;
import com.autou.emailai.domain.EmailCategory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class EmailAnalysisService implements EmailAnalysisUseCase {

    private static final String MSG_TEXT_REQUIRED = "Cole um texto de e-mail para analise.";
    private static final String MSG_FILE_REQUIRED = "Selecione um arquivo .txt ou .pdf para analise.";
    private static final String MSG_UNSUPPORTED_FILE = "Formato nao suportado. Use .txt ou .pdf.";
    private static final String MSG_EXTRACTION_FAILED = "Falha ao extrair texto do arquivo.";
    private static final String MSG_EMPTY_TEXT = "Nao foi possivel extrair texto do arquivo (PDF pode ser escaneado/imagem).";
    private static final String MSG_AI_NOT_CONFIGURED = "Chave de IA nao configurada.";
    private static final String MSG_AI_FAILURE = "Falha ao consultar a IA. Tente novamente.";
    private static final String MSG_AI_INVALID = "Resposta da IA invalida. Tente novamente.";
    private static final String MSG_AI_INVALID_CATEGORY = "Categoria retornada pela IA e invalida.";

    private final List<FileTextExtractor> extractors;
    private final AiClient aiClient;

    public EmailAnalysisService(
            List<FileTextExtractor> extractors,
            ObjectProvider<AiClient> aiClientProvider
    ) {
        this.extractors = extractors;
        this.aiClient = aiClientProvider.getIfAvailable();
    }

    @Override
    public EmailAnalysisResult analyzeText(String rawText) {
        String cleaned = (rawText == null) ? "" : rawText.strip();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(MSG_TEXT_REQUIRED);
        }
        return analyzeWithAi(cleaned);
    }

    @Override
    public EmailAnalysisResult analyzeFile(byte[] bytes, String filename, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new InvalidFileException(MSG_FILE_REQUIRED);
        }

        FileTextExtractor extractor = extractors.stream()
                .filter(e -> e.supports(filename, contentType))
                .findFirst()
                .orElse(null);

        if (extractor == null) {
            throw new InvalidFileException(MSG_UNSUPPORTED_FILE);
        }

        String extracted;
        try {
            extracted = extractor.extract(bytes);
        } catch (RuntimeException e) {
            throw new InvalidFileException(MSG_EXTRACTION_FAILED, e);
        }

        String cleaned = normalizeUnicode(extracted).strip();
        if (cleaned.isEmpty()) {
            throw new InvalidFileException(MSG_EMPTY_TEXT);
        }
        return analyzeWithAi(cleaned);
    }

    private EmailAnalysisResult toDomain(AiAnalysisResponse response) {
        if (response == null) {
            throw new InvalidAiResponseException(MSG_AI_INVALID);
        }

        EmailCategory category = resolveCategory(response.category());

        return new EmailAnalysisResult(
                category,
                response.confidence(),
                response.reason(),
                response.suggestedReply(),
                "openai"
        );
    }

    private EmailCategory resolveCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            throw new InvalidAiResponseException(MSG_AI_INVALID_CATEGORY);
        }
        try {
            return EmailCategory.valueOf(rawCategory.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidAiResponseException(MSG_AI_INVALID_CATEGORY, ex);
        }
    }

    private EmailAnalysisResult analyzeWithAi(String cleaned) {
        if (aiClient == null) {
            throw new AiNotConfiguredException(MSG_AI_NOT_CONFIGURED);
        }
        try {
            AiAnalysisResponse response = aiClient.analyze(cleaned);
            return toDomain(response);
        } catch (AiNotConfiguredException | AiQuotaException | AiRequestFailedException | InvalidAiResponseException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new AiRequestFailedException(MSG_AI_FAILURE, ex);
        }
    }

    private String normalizeUnicode(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        return normalized.replaceAll("[\\p{C}&&[^\\n\\r\\t]]", "");
    }
}
