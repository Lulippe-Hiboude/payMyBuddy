package com.lulippe.paymybuddy.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuthenticationUtilTest {
    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    void shouldReturnAuthenticatedUserEmail() {
        String email = AuthenticationUtil.getAuthenticatedUserEmail();
        assertEquals("test@test.com", email);
    }
}