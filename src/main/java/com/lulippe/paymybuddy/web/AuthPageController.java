package com.lulippe.paymybuddy.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthPageController {

    @GetMapping("/register")
    public String showRegisterForm() {
        log.info("showRegisterForm");
        return "register";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        log.info("showLoginForm");
        return "login";
    }
}
