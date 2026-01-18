package com.autou.emailai.application.ports.out;

import com.autou.emailai.domain.EmailAnalysisResult;

public interface AiClient {
    EmailAnalysisResult analyze(String cleanedEmailText);
}
