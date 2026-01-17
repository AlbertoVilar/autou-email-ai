package com.autou.emailai.domain;

public record EmailAnalysisResult(
        EmailCategory category,
        double confidence,
        String reason,
        String suggestedReply,
        String model
) {
}
