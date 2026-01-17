package com.autou.emailai.application.ports;

import com.autou.emailai.domain.EmailAnalysisResult;

public interface AiClient {
    EmailAnalysisResult analyze(String cleanedEmailText);
}
