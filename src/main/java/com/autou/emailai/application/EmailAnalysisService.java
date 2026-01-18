package com.autou.emailai.application;

import com.autou.emailai.application.ports.in.EmailAnalysisUseCase;
import com.autou.emailai.application.ports.out.AiClient;
import com.autou.emailai.application.ports.out.FileTextExtractor;
import com.autou.emailai.application.ports.out.dto.AiAnalysisResponse;
import com.autou.emailai.domain.EmailAnalysisResult;
import com.autou.emailai.domain.EmailCategory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class EmailAnalysisService implements EmailAnalysisUseCase {

    private static final int PREVIEW_LIMIT = 300;
    private static final String MSG_TEXT_REQUIRED = "Cole um texto de e-mail para analise.";
    private static final String MSG_FILE_REQUIRED = "Selecione um arquivo .txt ou .pdf para analise.";
    private static final String MSG_UNSUPPORTED_FILE = "Formato nao suportado. Use .txt ou .pdf.";
    private static final String MSG_EXTRACTION_FAILED = "Falha ao extrair texto do arquivo.";
    private static final String MSG_EMPTY_TEXT = "Nao foi possivel extrair texto do arquivo (PDF pode ser escaneado/imagem).";

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

        if (aiClient != null) {
            AiAnalysisResponse response = aiClient.analyze(cleaned);
            return toDomain(response);
        }

        return new EmailAnalysisResult(
                EmailCategory.PRODUTIVO,
                0.85,
                "Stub: exemplo de motivo curto.",
                "Stub: resposta sugerida para o e-mail.",
                "stub"
        );
    }

    @Override
    public EmailAnalysisResult analyzeFile(byte[] bytes, String filename, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException(MSG_FILE_REQUIRED);
        }

        FileTextExtractor extractor = extractors.stream()
                .filter(e -> e.supports(filename, contentType))
                .findFirst()
                .orElse(null);

        if (extractor == null) {
            throw new IllegalArgumentException(MSG_UNSUPPORTED_FILE);
        }

        String extracted;
        try {
            extracted = extractor.extract(bytes);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(MSG_EXTRACTION_FAILED, e);
        }

        String cleaned = normalizeUnicode(extracted).strip();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(MSG_EMPTY_TEXT);
        }

        if (aiClient != null) {
            AiAnalysisResponse response = aiClient.analyze(cleaned);
            return toDomain(response);
        }

        String preview = cleaned.length() > PREVIEW_LIMIT
                ? cleaned.substring(0, PREVIEW_LIMIT) + "..."
                : cleaned;

        return new EmailAnalysisResult(
                EmailCategory.PRODUTIVO,
                0.80,
                "Stub: extracao OK. Preview: " + preview,
                "Stub: resposta sugerida para o e-mail a partir do arquivo.",
                "stub"
        );
    }

    private EmailAnalysisResult toDomain(AiAnalysisResponse response) {
        if (response == null) {
            return new EmailAnalysisResult(
                    EmailCategory.PRODUTIVO,
                    0.0,
                    "",
                    "",
                    "unknown"
            );
        }

        EmailCategory category = resolveCategory(response.category());

        return new EmailAnalysisResult(
                category,
                response.confidence(),
                response.reason(),
                response.suggestedReply(),
                "ai"
        );
    }

    private EmailCategory resolveCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return EmailCategory.PRODUTIVO;
        }
        try {
            return EmailCategory.valueOf(rawCategory.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return EmailCategory.PRODUTIVO;
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
