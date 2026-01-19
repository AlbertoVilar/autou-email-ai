package com.autou.emailai.application.ports.out;

public interface FileTextExtractor {

    boolean supports(String filename, String contentType);

    String extract(byte[] bytes);
}
