package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("should handle register request")
    void shouldHandleRegisterRequest() {
        //given
        final String username = "username";
        final String password = "password";
        final String hashedPassword = "hashedPassword";
        final String email = "email@email.com";
        RegisterRequest.RoleEnum user = RegisterRequest.RoleEnum.USER;
        final RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        registerRequest.setEmail(email);
        registerRequest.setRole(user);

        doNothing().when(userService).ensureUsernameAndEmailAreUnique(registerRequest.getUsername(), registerRequest.getEmail());
        given(passwordEncoder.encode(registerRequest.getPassword())).willReturn(hashedPassword);
        doNothing().when(userService).createAppUser(username, email, hashedPassword, registerRequest.getRole());

        //when & then
        assertDoesNotThrow(() -> authService.handleRegisterRequest(registerRequest));
    }

    @Test
    @DisplayName("should throw EntityAlreadyExistsException")
    void shouldThrowEntityAlreadyExistsException() {
        //given
        final String username = "username";
        final String password = "password";
        final String hashedPassword = "hashedPassword";
        final String email = "email@email.com";
        RegisterRequest.RoleEnum user = RegisterRequest.RoleEnum.USER;
        final RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        registerRequest.setEmail(email);
        registerRequest.setRole(user);

        doThrow(EntityAlreadyExistsException.class).when(userService).ensureUsernameAndEmailAreUnique(registerRequest.getUsername(), registerRequest.getEmail());

        //when
        assertThrows(EntityAlreadyExistsException.class, () -> authService.handleRegisterRequest(registerRequest));

        //then
        verify(userService, times(0)).createAppUser(username, email, hashedPassword, registerRequest.getRole());
    }
}