package com.autou.emailai.web.controller;

import com.autou.emailai.application.ports.FileTextExtractor;
import com.autou.emailai.web.dto.AnalyzeResultViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class EmailUiController {

    private static final String INDEX_VIEW = "index";
    private static final int PREVIEW_LIMIT = 300;
    private final List<FileTextExtractor> extractors;

    public EmailUiController(List<FileTextExtractor> extractors) {
        this.extractors = extractors;
    }

    @GetMapping("/")
    public String index() {
        return INDEX_VIEW;
    }

    @PostMapping("/analyze-text")
    public String analyzeText(@RequestParam("text") String text, Model model) {
        if (text == null || text.isBlank()) {
            model.addAttribute("errorMessage", "Cole um texto de e-mail para análise.");
            model.addAttribute("result", null);
            return INDEX_VIEW;
        }

        // TODO: chamar EmailAnalysisService (mais tarde)
        var result = new AnalyzeResultViewModel(
                "PRODUTIVO",
                0.85,
                "Stub: exemplo de motivo curto.",
                "Stub: resposta sugerida para o e-mail."
        );
        model.addAttribute("result", result);
        return INDEX_VIEW;
    }

    @PostMapping("/analyze-file")
    public String analyzeFile(@RequestParam("file") MultipartFile file, Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("errorMessage", "Selecione um arquivo .txt ou .pdf para análise.");
            model.addAttribute("result", null);
            return INDEX_VIEW;
        }

        String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "arquivo";
        String contentType = file.getContentType();

        var extractor = extractors.stream()
                .filter(e -> e.supports(filename, contentType))
                .findFirst()
                .orElse(null);

        if (extractor == null) {
            model.addAttribute("errorMessage", "Formato não suportado. Use .txt ou .pdf.");
            model.addAttribute("result", null);
            return INDEX_VIEW;
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Não foi possível ler o arquivo enviado.");
            model.addAttribute("result", null);
            return INDEX_VIEW;
        }

        String extracted;
        try {
            extracted = extractor.extract(bytes);
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", "Falha ao extrair texto do arquivo.");
            model.addAttribute("result", null);
            return INDEX_VIEW;
        }

        String cleaned = (extracted == null) ? "" : extracted.strip();
        if (cleaned.isEmpty()) {
            model.addAttribute(
                    "errorMessage",
                    "Não foi possível extrair texto do arquivo (PDF pode ser escaneado/imagem)."
            );
            model.addAttribute("result", null);
            return INDEX_VIEW;
        }

        String preview = cleaned.length() > PREVIEW_LIMIT
                ? cleaned.substring(0, PREVIEW_LIMIT) + "..."
                : cleaned;

        AnalyzeResultViewModel result = new AnalyzeResultViewModel(
                "PRODUTIVO",
                0.80,
                "Stub: extração OK. Preview: " + preview,
                "Stub: resposta sugerida para o e-mail a partir do arquivo."
        );

        model.addAttribute("result", result);
        return INDEX_VIEW;
    }
}
