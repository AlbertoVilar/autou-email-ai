package com.autou.emailai.application;

import com.autou.emailai.domain.EmailAnalysisResult;

public interface EmailAnalysisService {
    EmailAnalysisResult analyzeText(String rawText);

    EmailAnalysisResult analyzeFile(byte[] bytes, String filename, String contentType);
}
