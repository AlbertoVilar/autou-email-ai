package com.autou.emailai.application.ports.out;

import com.autou.emailai.application.ports.out.dto.AiAnalysisResponse;

public interface AiClient {
    AiAnalysisResponse analyze(String cleanedEmailText);
}
