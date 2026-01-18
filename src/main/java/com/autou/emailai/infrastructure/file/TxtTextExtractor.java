package com.autou.emailai.infrastructure.file;

import com.autou.emailai.application.ports.out.FileTextExtractor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class TxtTextExtractor implements FileTextExtractor {

    @Override
    public boolean supports(String filename, String contentType) {
        if (filename != null && filename.toLowerCase().endsWith(".txt")) {
            return true;
        }
        return contentType != null && contentType.equalsIgnoreCase("text/plain");
    }

    @Override
    public String extract(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
