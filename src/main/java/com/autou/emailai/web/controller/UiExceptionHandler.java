package com.autou.emailai.web.controller;

import com.autou.emailai.application.exception.AiNotConfiguredException;
import com.autou.emailai.application.exception.AiQuotaException;
import com.autou.emailai.application.exception.AiRequestFailedException;
import com.autou.emailai.application.exception.InvalidAiResponseException;
import com.autou.emailai.application.exception.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class UiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiExceptionHandler.class);
    private static final String INDEX_VIEW = "index";
    private static final String MSG_UNEXPECTED = "Erro inesperado. Tente novamente.";

    @ExceptionHandler({
            AiNotConfiguredException.class,
            AiQuotaException.class,
            AiRequestFailedException.class,
            InvalidAiResponseException.class,
            InvalidFileException.class,
            IllegalArgumentException.class
    })
    public String handleKnownExceptions(RuntimeException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("result", null);
        return INDEX_VIEW;
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception ex, Model model) {
        LOGGER.error("Unexpected error in UI flow", ex);
        model.addAttribute("errorMessage", MSG_UNEXPECTED);
        model.addAttribute("result", null);
        return INDEX_VIEW;
    }
}
