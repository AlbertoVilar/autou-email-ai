package com.autou.emailai.infrastructure.file;

import com.autou.emailai.application.ports.FileTextExtractor;

public class TxtTextExtractor implements FileTextExtractor {

    @Override
    public boolean supports(String filename, String contentType) {
        return false;
    }

    @Override
    public String extract(byte[] bytes) {
        return "";
    }
}
