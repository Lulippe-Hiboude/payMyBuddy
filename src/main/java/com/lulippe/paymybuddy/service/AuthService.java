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

    /**
     * Handles a user registration request.
     * <p>
     * This method ensures that the provided username and email are unique,
     * encodes the raw password, and delegates the user creation to {@link UserService}.
     * </p>
     *
     * @param registerRequest the {@link RegisterRequest} containing the user's registration details
     *                        (username, email, raw password, and role)
     * @throws com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException if a user with the same username or email already exists
     */
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
