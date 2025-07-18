package com.lulippe.paymybuddy.api.controller;

import com.lulippe.paymybuddy.service.AuthService;
import com.lulippe.paymybuddy.user.api.AuthentificationApi;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class
AuthController implements AuthentificationApi {
    private final AuthService authService;

    @Override
    public ResponseEntity<String> registerNewUser(final RegisterRequest registerRequest) {
        log.info("Registering new user: {}", registerRequest);
        authService.handleRegisterRequest(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("user registered successfully");
    }
}
