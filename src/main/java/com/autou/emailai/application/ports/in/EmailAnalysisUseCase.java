package com.autou.emailai.application.ports.in;

import com.autou.emailai.domain.EmailAnalysisResult;

public interface EmailAnalysisUseCase {

    EmailAnalysisResult analyzeText(String rawText);

    EmailAnalysisResult analyzeFile(byte[] bytes, String filename, String contentType);
}
