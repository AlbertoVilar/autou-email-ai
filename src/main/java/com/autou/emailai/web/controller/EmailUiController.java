package com.autou.emailai.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EmailUiController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
