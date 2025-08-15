package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.api.exception.NonexistentEntityException;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.enums.Role;
import com.lulippe.paymybuddy.persistence.repository.AppUserRepository;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<AppUser> userArgumentCaptor;

    @Test
    @DisplayName("should do nothing if username and email are unique")
    void shouldDoNothingIfUsernameAndEmailAreUnique() {
        //given
        final String username = "test";
        final String email = "test@test.com";
        given(appUserRepository.existsByUsernameOrEmail(username,email)).willReturn(false);

        //when & then
        assertDoesNotThrow(() ->userService.ensureUsernameAndEmailAreUnique(username,email));
    }

    @Test
    @DisplayName("should throw EntityAlreadyExistsException if username or email already exists")
    void shouldThrowEntityAlreadyExistsExceptionIfUsernameOrEmailAlreadyExists() {
        //given
        final String username = "test";
        final String email = "test@test.com";
        given(appUserRepository.existsByUsernameOrEmail(username,email)).willReturn(true);
        //when & then
        assertThrows(EntityAlreadyExistsException.class, () ->userService.ensureUsernameAndEmailAreUnique(username,email));
    }

    @Test
    @DisplayName("should create an appUser")
    void shouldCreateAppUser() {
        //given
        final String username = "username";
        final String email = "test@email.com";
        final String hashedPassword = "hashedPassword";
        final RegisterRequest.RoleEnum role = RegisterRequest.RoleEnum.USER;

        //when
        userService.createAppUser(username, email, hashedPassword, role);

        //then
        verify(appUserRepository,(times(1))).save(userArgumentCaptor.capture());
        final AppUser appUser = userArgumentCaptor.getValue();
        assertEquals(username,appUser.getUsername());
        assertEquals(email,appUser.getEmail());
        assertEquals(hashedPassword,appUser.getPassword());
        assertEquals(Role.USER,appUser.getRole());
        assertEquals("ROLE_USER",appUser.getRole().getRoleName());
    }

    @Test
    @DisplayName("should return a appuser by email")
    void shouldReturnAppUserByEmail() {
        //given
        final String email = "test@email.com";
        final AppUser appUser = AppUser.builder()
                .email(email)
                .role(Role.USER)
                .password("hashedPassword")
                .account(BigDecimal.TEN)
                .build();
        given(appUserRepository.findByEmail(email)).willReturn(Optional.of(appUser));

        //when
        final AppUser expected = userService.getAppUserByEmail(email);

        //then
        assertEquals(expected,appUser);
    }

    @Test
    @DisplayName("should throw EntityNotFoundException if appUser does not exist")
    void shouldThrowInexistantEntityExceptionIfAppUserDoesNotExist() {
        //given
        final String email = "test@email.com";
        given(appUserRepository.findByEmail(email)).willReturn(Optional.empty());

        //when & then
        NonexistentEntityException exception = assertThrows (NonexistentEntityException.class, () ->userService.getAppUserByEmail(email));
        assertEquals("User with email test@email.com does not exist",exception.getMessage());
    }
}