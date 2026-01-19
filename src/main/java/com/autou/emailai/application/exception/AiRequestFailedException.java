package com.autou.emailai.application.exception;

public class AiRequestFailedException extends RuntimeException {
    public AiRequestFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
