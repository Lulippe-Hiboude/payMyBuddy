package com.lulippe.paymybuddy.web;

import com.lulippe.paymybuddy.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthPageController {
    private final AuthService authService;

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }
}
