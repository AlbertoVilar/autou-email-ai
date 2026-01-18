package com.autou.emailai.web.controller;

import com.autou.emailai.application.ports.in.EmailAnalysisUseCase;
import com.autou.emailai.domain.EmailAnalysisResult;
import com.autou.emailai.web.dto.AnalyzeResultViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class EmailUiController {

    private static final String INDEX_VIEW = "index";
    private static final String MSG_FILE_REQUIRED = "Selecione um arquivo .txt ou .pdf para anÇ­lise.";
    private static final String MSG_FILE_READ_FAILED = "NÇœo foi possÇðvel ler o arquivo enviado.";

    private final EmailAnalysisUseCase emailAnalysisUseCase;

    public EmailUiController(EmailAnalysisUseCase emailAnalysisUseCase) {
        this.emailAnalysisUseCase = emailAnalysisUseCase;
    }

    @GetMapping("/")
    public String index() {
        return INDEX_VIEW;
    }

    @PostMapping("/analyze-text")
    public String analyzeText(@RequestParam("text") String text, Model model) {
        try {
            EmailAnalysisResult result = emailAnalysisUseCase.analyzeText(text);
            model.addAttribute("result", toViewModel(result));
            return INDEX_VIEW;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("result", null);
            return INDEX_VIEW;
        }
    }

    @PostMapping("/analyze-file")
    public String analyzeFile(@RequestParam("file") MultipartFile file, Model model) {
        if (file == null || file.isEmpty()) {
            model.addAttribute("errorMessage", MSG_FILE_REQUIRED);
            model.addAttribute("result", null);
            return INDEX_VIEW;
        }

        String filename = (file.getOriginalFilename() != null) ? file.getOriginalFilename() : "arquivo";
        String contentType = file.getContentType();

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            model.addAttribute("errorMessage", MSG_FILE_READ_FAILED);
            model.addAttribute("result", null);
            return INDEX_VIEW;
        }

        try {
            EmailAnalysisResult result = emailAnalysisUseCase.analyzeFile(bytes, filename, contentType);
            model.addAttribute("result", toViewModel(result));
            return INDEX_VIEW;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("result", null);
            return INDEX_VIEW;
        }
    }

    private AnalyzeResultViewModel toViewModel(EmailAnalysisResult result) {
        return new AnalyzeResultViewModel(
                result.category().name(),
                result.confidence(),
                result.reason(),
                result.suggestedReply()
        );
    }
}
