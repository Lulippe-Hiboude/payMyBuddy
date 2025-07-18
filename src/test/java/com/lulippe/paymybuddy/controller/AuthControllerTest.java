package com.lulippe.paymybuddy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulippe.paymybuddy.TestSecurityConfig;
import com.lulippe.paymybuddy.api.controller.AuthController;
import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.service.AuthService;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("should register a new user")
    void shouldRegisterNewUser() throws Exception {
        //given
        final String username = "username";
        final String password = "password";
        final String email = "email@email.com";
        RegisterRequest.RoleEnum user = RegisterRequest.RoleEnum.USER;
        final RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        registerRequest.setEmail(email);
        registerRequest.setRole(user);

        doNothing().when(authService).handleRegisterRequest(registerRequest);

        //when & then
        mockMvc.perform(post("/auth/register/v0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("user registered successfully"));
    }

    @Test
    @DisplayName("should throw EntityAlreadyExistsException")
    void shouldThrowEntityAlreadyExistsException() throws Exception {
        //given
        final String username = "username";
        final String password = "password";
        final String email = "email@email.com";
        RegisterRequest.RoleEnum user = RegisterRequest.RoleEnum.USER;
        final RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        registerRequest.setEmail(email);
        registerRequest.setRole(user);

        doThrow(new EntityAlreadyExistsException("A user with this email or username already exists")).when(authService).handleRegisterRequest(registerRequest);

        //when & then
        mockMvc.perform(post("/auth/register/v0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().string("A user with this email or username already exists"));
    }
}