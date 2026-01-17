package com.autou.emailai.web.dto;

public record AnalyzeResultViewModel(
        String category,
        double confidence,
        String reason,
        String suggestedReply
) {
}
