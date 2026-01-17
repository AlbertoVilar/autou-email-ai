package com.autou.emailai.web.controller;

import com.autou.emailai.web.dto.AnalyzeResultViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EmailUiController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/analyze-text")
    public String analyzeText(@RequestParam("text") String text, Model model) {
        // TODO: validar texto vazio
        // TODO: chamar EmailAnalysisService (mais tarde)
       var result = new AnalyzeResultViewModel(
                "PRODUTIVO",
                0.85,
                "Stub: exemplo de motivo curto.",
                "Stub: resposta sugerida para o e-mail."
        );
        model.addAttribute("result", result);
        return "index";
    }

}
