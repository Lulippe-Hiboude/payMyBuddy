package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.user.model.RegisterRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public void handleRegisterRequest(final RegisterRequest registerRequest) {
        userService.ensureUsernameAndEmailAreUnique(registerRequest.getUsername(), registerRequest.getEmail());
        final String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());
        userService.createAppUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                hashedPassword,
                registerRequest.getRole());
    }
}
