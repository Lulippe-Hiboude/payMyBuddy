package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.api.exception.InexistantEntityException;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferRequest;
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
import java.util.*;

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
        InexistantEntityException exception = assertThrows (InexistantEntityException.class, () ->userService.getAppUserByEmail(email));
        assertEquals("User with email test@email.com does not exist",exception.getMessage());
    }

    @Test
    @DisplayName("Should perform Bank Transfer with half even rounding")
    void shouldPerformBankTransfer() {
        //given
        final String email = "test@email.com";
        final AppUser user = AppUser.builder()
                .email(email)
                .role(Role.USER)
                .password("hashedPassword")
                .account(BigDecimal.TEN)
                .build();
        final BankTransferRequest request = new BankTransferRequest();
        request.setAmount(20.126);
        request.setBankHolder("testBankHolder");

        //when
        userService.performBankTransfer(user,request);

        //then
        verify(appUserRepository,(times(1))).save(userArgumentCaptor.capture());
        final AppUser updateUser = userArgumentCaptor.getValue();
        assertEquals(email,updateUser.getEmail());
        assertEquals(new BigDecimal("30.13"),updateUser.getAccount());
    }

    @Test
    @DisplayName("should add a new friend")
    void shouldAddNewFriend() {
        //given
        final String userEmail = "test@email.com";
        final String friendEmail = "friend@email.com";
        final Set<AppUser> userFriends = new HashSet<>();
        final Set<AppUser> friendFriends = new HashSet<>();
        final String userPassword = "hashedPassword";
        final AppUser currentUser = AppUser.builder()
                .email(userEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(userFriends)
                .build();
        final AppUser friendUser = AppUser.builder()
                .email(friendEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(friendFriends)
                .build();
        given(appUserRepository.findByEmail(userEmail)).willReturn(Optional.of(currentUser));
        given(appUserRepository.findByEmail(friendEmail)).willReturn(Optional.of(friendUser));

        //when
        userService.handleFriendAddition(userEmail,friendEmail);

        //then
        verify(appUserRepository,(times(1))).save(userArgumentCaptor.capture());
        final AppUser updateUser = userArgumentCaptor.getValue();
        assertEquals(userEmail,updateUser.getEmail());
        assertNotNull(updateUser.getFriends());
        assertTrue(updateUser.getFriends().contains(friendUser));
    }

    @Test
    @DisplayName("should throw an IllegalArgumentException if self adding")
    void shouldThrowIllegalArgumentExceptionIfSelfAdding() {
        //given
        final String userEmail = "test@email.com";
        final Set<AppUser> userFriends = new HashSet<>();
        final String userPassword = "hashedPassword";
        final AppUser currentUser = AppUser.builder()
                .email(userEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(userFriends)
                .build();
        given(appUserRepository.findByEmail(userEmail)).willReturn(Optional.of(currentUser));
        given(appUserRepository.findByEmail(userEmail)).willReturn(Optional.of(currentUser));

        //when
        assertThrows(IllegalArgumentException.class, () -> userService.handleFriendAddition(userEmail,userEmail));
    }

    @Test
    @DisplayName("should throw EntityAlreadyExistsException if friend already added in friendList")
    void shouldThrowEntityAlreadyExistsExceptionIfFriendAlreadyAddedInFriendList() {
        //given
        final String userEmail = "test@email.com";
        final String friendEmail = "friend@email.com";
        final String userPassword = "hashedPassword";
        final Set<AppUser> friendFriends = new HashSet<>();
        final AppUser friendUser = AppUser.builder()
                .email(friendEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(friendFriends)
                .build();
        final Set<AppUser> userFriends = new HashSet<>();
        userFriends.add(friendUser);
        final AppUser currentUser = AppUser.builder()
                .email(userEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(userFriends)
                .build();

        given(appUserRepository.findByEmail(userEmail)).willReturn(Optional.of(currentUser));
        given(appUserRepository.findByEmail(friendEmail)).willReturn(Optional.of(friendUser));

        //when
        assertThrows(EntityAlreadyExistsException.class, () ->userService.handleFriendAddition(userEmail,friendEmail));

    }
}