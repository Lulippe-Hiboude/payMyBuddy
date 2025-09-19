package com.lulippe.paymybuddy.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.enums.Role;
import com.lulippe.paymybuddy.persistence.repository.AppUserRepository;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class AuthControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("shouldRegisterNewUser")
    void registerNewUser() throws Exception {
        //given
        final String username ="Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password ="123";
        final RegisterRequest registerRequest= createRegisterRequest(username, email, password);

        final String expectedJson = "user registered successfully";

        //when
        mockMvc.perform(post("/auth/register/v0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.content().string(expectedJson));

        //then
        final AppUser savedUser = appUserRepository.findByUsername(username).orElseThrow(() -> new AssertionError("user not found"));
        assertEquals(username,savedUser.getUsername()) ;
        assertEquals(email,savedUser.getEmail()) ;
        assertEquals(Role.USER,savedUser.getRole()) ;
        assertFalse(savedUser.isSystemAccount());
    }

    @Test
    @DisplayName("should return 409 if user already exists")
    void shouldReturn409IfUserAlreadyExists() throws Exception {
        final String username ="Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password ="123";
        final RegisterRequest registerRequest1 = createRegisterRequest(username, email, password);
        final RegisterRequest registerRequest2 = createRegisterRequest(username, email, password);

        final String expectedJson1 = "user registered successfully";
        final String expectedJson2 = "A user with this email or username already exists";

        mockMvc.perform(post("/auth/register/v0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest1)))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.content().string(expectedJson1));
        //when
        mockMvc.perform(post("/auth/register/v0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest2)))
                .andExpect(status().isConflict())
                .andExpect(MockMvcResultMatchers.content().string(expectedJson2));
    }

    private RegisterRequest createRegisterRequest(final String username, final String email, final String password) {

        final RegisterRequest.RoleEnum role = RegisterRequest.RoleEnum.USER;
        final RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        registerRequest.setRole(role);
        return registerRequest;
    }
}
