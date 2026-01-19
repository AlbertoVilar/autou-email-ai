package com.autou.emailai.application.ports.out.dto;

public record AiAnalysisResponse(
        String category,
        double confidence,
        String reason,
        String suggestedReply
) {
}
