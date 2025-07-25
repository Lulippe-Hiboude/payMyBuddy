package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.enums.Role;
import com.lulippe.paymybuddy.persistence.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {
    @Mock
    private AppUserRepository appUserRepository;
    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    @Test
    @DisplayName("should load user by email")
    void shouldLoadUserByUsername() {
        //given
        final String username = "username";
        String email = "username@email.com";
        Role userRole = Role.USER;
        String hashedPassword = "hashedPassword";
        final AppUser appUser = AppUser.builder()
                .username(username)
                .email(email)
                .role(userRole)
                .password(hashedPassword)
                .build();
        given(appUserRepository.findByEmail(username)).willReturn(Optional.of(appUser));
        //when
        final UserDetails userDetails = appUserDetailsService.loadUserByUsername(username);

        //then
        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        assertEquals(hashedPassword, userDetails.getPassword());
        assertNotNull(userDetails.getAuthorities());
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException")
    void shouldThrowUsernameNotFoundException() {
        //given
        final String username = "username";
        given(appUserRepository.findByEmail(username)).willReturn(Optional.empty());

        //when & then
        assertThrows(UsernameNotFoundException.class, () -> appUserDetailsService.loadUserByUsername(username));

    }
}