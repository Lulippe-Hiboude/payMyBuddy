package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.api.exception.InsufficientFundsException;
import com.lulippe.paymybuddy.api.exception.NonexistentEntityException;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferRequest;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.enums.Role;
import com.lulippe.paymybuddy.persistence.repository.AppUserRepository;
import com.lulippe.paymybuddy.user.model.InformationsToUpdate;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import com.lulippe.paymybuddy.user.model.UserFriend;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
        given(appUserRepository.existsByUsernameOrEmail(username, email)).willReturn(false);

        //when & then
        assertDoesNotThrow(() -> userService.ensureUsernameAndEmailAreUnique(username, email));
    }

    @Test
    @DisplayName("should throw EntityAlreadyExistsException if username or email already exists")
    void shouldThrowEntityAlreadyExistsExceptionIfUsernameOrEmailAlreadyExists() {
        //given
        final String username = "test";
        final String email = "test@test.com";
        given(appUserRepository.existsByUsernameOrEmail(username, email)).willReturn(true);
        //when & then
        assertThrows(EntityAlreadyExistsException.class, () -> userService.ensureUsernameAndEmailAreUnique(username, email));
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
        verify(appUserRepository, (times(1))).save(userArgumentCaptor.capture());
        final AppUser appUser = userArgumentCaptor.getValue();
        assertEquals(username, appUser.getUsername());
        assertEquals(email, appUser.getEmail());
        assertEquals(hashedPassword, appUser.getPassword());
        assertEquals(Role.USER, appUser.getRole());
        assertEquals("ROLE_USER", appUser.getRole().getRoleName());
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
        assertEquals(expected, appUser);
    }

    @Test
    @DisplayName("should throw EntityNotFoundException if appUser does not exist")
    void shouldThrowInexistantEntityExceptionIfAppUserDoesNotExist() {
        //given
        final String email = "test@email.com";
        given(appUserRepository.findByEmail(email)).willReturn(Optional.empty());

        //when & then
        NonexistentEntityException exception = assertThrows(NonexistentEntityException.class, () -> userService.getAppUserByEmail(email));
        assertEquals("User with email test@email.com does not exist", exception.getMessage());
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
        userService.performBankTransfer(user, request);

        //then
        verify(appUserRepository, (times(1))).save(userArgumentCaptor.capture());
        final AppUser updateUser = userArgumentCaptor.getValue();
        assertEquals(email, updateUser.getEmail());
        assertEquals(new BigDecimal("30.13"), updateUser.getAccount());
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
        userService.handleFriendAddition(userEmail, friendEmail);

        //then
        verify(appUserRepository, (times(1))).save(userArgumentCaptor.capture());
        final AppUser updateUser = userArgumentCaptor.getValue();
        assertEquals(userEmail, updateUser.getEmail());
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
        assertThrows(IllegalArgumentException.class, () -> userService.handleFriendAddition(userEmail, userEmail));
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
        assertThrows(EntityAlreadyExistsException.class, () -> userService.handleFriendAddition(userEmail, friendEmail));
    }

    @Test
    @DisplayName("should throw an IllegalArgumentException if user tries to add System as a friend")
    void shouldThrowIllegalArgumentExceptionIfUserTriesToAddSystemAsAFriend() {
        //given
        final String userEmail = "test@email.com";
        final String friendEmail = "platform@paymybuddy.com";
        final String userPassword = "hashedPassword";
        final Set<AppUser> userFriends = new HashSet<>();
        final AppUser currentUser = AppUser.builder()
                .email(userEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(userFriends)
                .systemAccount(false)
                .build();

        final AppUser friendUser = AppUser.builder()
                .email(friendEmail)
                .role(Role.ROLE_SYSTEM)
                .systemAccount(true)
                .build();
        given(appUserRepository.findByEmail(userEmail)).willReturn(Optional.of(currentUser));
        given(appUserRepository.findByEmail(friendEmail)).willReturn(Optional.of(friendUser));

        //when
        assertThrows(IllegalArgumentException.class, () -> userService.handleFriendAddition(userEmail,friendEmail));
    }

    @Test
    @DisplayName("should do nothing if receiver is a friend with sender")
    void shouldDoNothingIfReceiverIsFriendWithSender() {
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

        //when & then
        assertDoesNotThrow(() -> userService.checkIfReceiverIsAFriend(friendUser, currentUser));
    }

    @Test
    @DisplayName("should throw illegalArgumentException if receiver is not a friend of sender")
    void shouldThrowIllegalArgumentExceptionIfReceiverIsNotAFriendOfSender() {
        //given
        final String userEmail = "test@email.com";
        final String friendEmail = "friend@email.com";
        final String userPassword = "hashedPassword";
        final Set<AppUser> friendFriends = new HashSet<>();
        final AppUser sender = AppUser.builder()
                .email(friendEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(friendFriends)
                .build();
        final Set<AppUser> userFriends = new HashSet<>();
        userFriends.add(sender);
        final AppUser receiver = AppUser.builder()
                .email(userEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(userFriends)
                .build();

        //when & then
        assertThrows(IllegalArgumentException.class, () -> userService.checkIfReceiverIsAFriend(receiver, sender));
    }

    @Test
    @DisplayName("should get user by username")
    void shouldGetUserByUsername() {
        //given
        final String userEmail = "test@email.com";
        final String userPassword = "hashedPassword";
        final String username = "username";
        final AppUser currentUser = AppUser.builder()
                .username(username)
                .email(userEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        given(appUserRepository.findByUsername(username)).willReturn(Optional.of(currentUser));

        //when
        final AppUser expected = userService.getAppUserByName(username);

        //then
        assertEquals(expected, currentUser);
    }

    @Test
    @DisplayName("should throw an exception if no user found")
    void shouldThrowAnExceptionIfNoUserFound() {
        //given

        final String username = "username";

        given(appUserRepository.findByUsername(username)).willReturn(Optional.empty());

        //when
        assertThrows(NonexistentEntityException.class, () -> userService.getAppUserByName(username));
    }

    @Test
    @DisplayName("should save sender and receiver")
    void shouldSaveSenderAndReceiver() {
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

        //when
        userService.saveTransactionAppUsers(friendUser, currentUser);

        //then
        verify(appUserRepository, times(1)).save(currentUser);
        verify(appUserRepository, times(1)).save(friendUser);
    }

    @Test
    @DisplayName("Should add commission to account system")
    void shouldAddCommissionToAccountSystem() {
        // given
        final BigDecimal commission = BigDecimal.TEN;
        final String systemEmail = "platform@paymybuddy.com";
        final BigDecimal systemAccount = BigDecimal.valueOf(200.00);
        final AppUser system = AppUser.builder()
                .email(systemEmail)
                .role(Role.ROLE_SYSTEM)
                .systemAccount(true)
                .account(systemAccount)
                .build();
        final BigDecimal expectedAccountSystem = systemAccount.add(commission);
        given(appUserRepository.findBySystemAccountTrue()).willReturn(Optional.of(system));

        //when
        userService.handleSystemAccount(commission);

        //then
        verify(appUserRepository, times(1)).save(userArgumentCaptor.capture());
        final AppUser expectedSystem = userArgumentCaptor.getValue();
        assertEquals(expectedSystem.getAccount(), expectedAccountSystem );
    }

    @Test
    @DisplayName("should throw an NonexistentEntityException if no system found")
    void shouldThrowAnNonExistentEntityExceptionIfNoSystemFound() {
        //given
        final BigDecimal commission = BigDecimal.TEN;
        given(appUserRepository.findBySystemAccountTrue()).willReturn(Optional.empty());

        assertThrows(NonexistentEntityException.class, () -> userService.handleSystemAccount(commission));
    }

    @Test
    @DisplayName("should return a list of UserFriend sorted alphabetically")
    void shouldReturnAListOfUserFriendSortedAlphabetically() {
        //given
        final String userEmail = "test@email.com";
        final String userPassword = "hashedPassword";
        final String friendName1 = "Albert";
        final String friendName2 = "Bernard";
        final AppUser friendUser1 = AppUser.builder()
                .username(friendName1)
                .email("friend1@email.com")
                .role(Role.USER)
                .build();
        final AppUser friendUser2 = AppUser.builder()
                .username(friendName2)
                .email("friend2@email.com")
                .role(Role.USER)
                .build();

        final Set<AppUser> userFriends = Set.of(friendUser1, friendUser2);
        final AppUser currentUser = AppUser.builder()
                .email(userEmail)
                .role(Role.USER)
                .password(userPassword)
                .friends(userFriends)
                .build();
        final UserFriend userFriend1 = new UserFriend();
        userFriend1.setFriendName(friendName1);

        final UserFriend userFriend2 = new UserFriend();
        userFriend2.setFriendName(friendName2);

        final List<UserFriend> expected = List.of(userFriend1, userFriend2);
        given(appUserRepository.findByEmail(userEmail)).willReturn(Optional.of(currentUser));
        //when
        final List<UserFriend> result = userService.getAllUserFriend(userEmail);
        //then
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("should return an empty list if no friend")
    void shouldReturnAnEmptyListIfNoFriend() {
        //given
        final String userEmail = "test@email.com";
        final String userPassword = "hashedPassword";
        final Set<AppUser> userFriends = Collections.emptySet();
        final AppUser currentUser = AppUser.builder()
                .email(userEmail)
                .role(Role.USER)
                .password(userPassword)
                .friends(userFriends)
                .build();
        given(appUserRepository.findByEmail(userEmail)).willReturn(Optional.of(currentUser));
        //when
        final List<UserFriend> result = userService.getAllUserFriend(userEmail);

        //then
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("should withdraw the amount to bank")
    void shouldWithdrawTheAmountToBank() {
        //given
        final String userEmail = "test@email.com";
        final String userPassword = "hashedPassword";
        final AppUser currentUser = AppUser.builder()
                .email(userEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        final BigDecimal amountToWithdraw = BigDecimal.valueOf(5.00);

        //when
        userService.withdrawToBank(currentUser, amountToWithdraw);

        //then
        verify(appUserRepository, times(1)).save(userArgumentCaptor.capture());
        final AppUser expectedUser = userArgumentCaptor.getValue();
        assertEquals(expectedUser.getAccount(), amountToWithdraw);
    }

    @Test
    @DisplayName("should throw InsufficientFundsException if amount to withdraw is too much")
    void shouldThrowInsufficientFundsExceptionIfInsufficientFundsException() {
        //given
        final String userEmail = "test@email.com";
        final String userPassword = "hashedPassword";
        final AppUser currentUser = AppUser.builder()
                .email(userEmail)
                .role(Role.USER)
                .password(userPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        final BigDecimal amountToWithdraw = BigDecimal.valueOf(15.00);

        //when & then
        assertThrows(InsufficientFundsException.class, () -> userService.withdrawToBank(currentUser, amountToWithdraw));
        verify(appUserRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("should update user information")
    void shouldUpdateUserInformation() {
        //given
        final String oldUserEmail = "oldEmail@email.com";
        final String newUserEmail = "newEmail@email.com";
        final String oldPassword = "oldHashedPassword";
        final String newPassword = "newHashedPassword";
        final String oldUsername = "oldUsername";
        final String newUsername = "newUsername";

        final InformationsToUpdate informationsToUpdate = new InformationsToUpdate();
        informationsToUpdate.setEmail(newUserEmail);
        informationsToUpdate.setPassword(newPassword);
        informationsToUpdate.setUsername(newUsername);

        final AppUser currentUser = AppUser.builder()
                .email(oldUserEmail)
                .username(oldUsername)
                .role(Role.USER)
                .password(oldPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        given(appUserRepository.findByEmail(oldUserEmail)).willReturn(Optional.of(currentUser));
        given(passwordEncoder.encode(newPassword)).willReturn(newPassword);

        //when
        userService.updateUserProfil(oldUserEmail,informationsToUpdate);
        verify(appUserRepository, times(1)).save(userArgumentCaptor.capture());
        final AppUser expectedUser = userArgumentCaptor.getValue();
        assertEquals(newUsername, expectedUser.getUsername());
        assertEquals(newPassword, expectedUser.getPassword());
        assertEquals(newUserEmail, expectedUser.getEmail());
        assertEquals(currentUser.getAccount(), expectedUser.getAccount());
        assertEquals(currentUser.getFriends(), expectedUser.getFriends());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException if email is empty")
    void shouldThrowIllegalArgumentExceptionIfEmailIsEmpty() {
        //given
        final String oldUserEmail = "oldEmail@email.com";
        final String newUserEmail = "  ";
        final String oldPassword = "oldHashedPassword";
        final String newPassword = "newHashedPassword";
        final String oldUsername = "oldUsername";
        final String newUsername = "newUsername";

        final InformationsToUpdate informationsToUpdate = new InformationsToUpdate();
        informationsToUpdate.setEmail(newUserEmail);
        informationsToUpdate.setPassword(newPassword);
        informationsToUpdate.setUsername(newUsername);

        final AppUser currentUser = AppUser.builder()
                .email(oldUserEmail)
                .username(oldUsername)
                .role(Role.USER)
                .password(oldPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        given(appUserRepository.findByEmail(oldUserEmail)).willReturn(Optional.of(currentUser));

        //when & then
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserProfil(oldUserEmail,informationsToUpdate));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException if password is empty")
    void shouldThrowIllegalArgumentExceptionIfPasswordIsEmpty() {
        //given
        final String oldUserEmail = "oldEmail@email.com";
        final String newUserEmail = "newEmail@email.com";
        final String oldPassword = "oldHashedPassword";
        final String newPassword = "  ";
        final String oldUsername = "oldUsername";
        final String newUsername = "newUsername";

        final InformationsToUpdate informationsToUpdate = new InformationsToUpdate();
        informationsToUpdate.setEmail(newUserEmail);
        informationsToUpdate.setPassword(newPassword);
        informationsToUpdate.setUsername(newUsername);

        final AppUser currentUser = AppUser.builder()
                .email(oldUserEmail)
                .username(oldUsername)
                .role(Role.USER)
                .password(oldPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        given(appUserRepository.findByEmail(oldUserEmail)).willReturn(Optional.of(currentUser));

        //when & then
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserProfil(oldUserEmail,informationsToUpdate));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException if username is empty")
    void shouldThrowIllegalArgumentExceptionIfUsernameIsEmpty() {
        //given
        final String oldUserEmail = "oldEmail@email.com";
        final String newUserEmail = "newEmail@email.com";
        final String oldPassword = "oldHashedPassword";
        final String newPassword = "newHashedPassword";
        final String oldUsername = "oldUsername";
        final String newUsername = "  ";

        final InformationsToUpdate informationsToUpdate = new InformationsToUpdate();
        informationsToUpdate.setEmail(newUserEmail);
        informationsToUpdate.setPassword(newPassword);
        informationsToUpdate.setUsername(newUsername);

        final AppUser currentUser = AppUser.builder()
                .email(oldUserEmail)
                .username(oldUsername)
                .role(Role.USER)
                .password(oldPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        given(appUserRepository.findByEmail(oldUserEmail)).willReturn(Optional.of(currentUser));

        //when & then
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserProfil(oldUserEmail,informationsToUpdate));
    }

    @Test
    @DisplayName("should update only username")
    void shouldUpdateOnlyUsername() {
        //given
        final String oldUserEmail = "oldEmail@email.com";
        final String newUserEmail = null;
        final String oldPassword = "oldHashedPassword";
        final String newPassword = null;
        final String oldUsername = "oldUsername";
        final String newUsername = "newUsername";

        final InformationsToUpdate informationsToUpdate = new InformationsToUpdate();
        informationsToUpdate.setEmail(newUserEmail);
        informationsToUpdate.setPassword(newPassword);
        informationsToUpdate.setUsername(newUsername);

        final AppUser currentUser = AppUser.builder()
                .email(oldUserEmail)
                .username(oldUsername)
                .role(Role.USER)
                .password(oldPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        given(appUserRepository.findByEmail(oldUserEmail)).willReturn(Optional.of(currentUser));

        //when
        userService.updateUserProfil(oldUserEmail,informationsToUpdate);
        verify(appUserRepository, times(1)).save(userArgumentCaptor.capture());
        final AppUser expectedUser = userArgumentCaptor.getValue();
        assertEquals(newUsername, expectedUser.getUsername());
        assertEquals(oldPassword, expectedUser.getPassword());
        assertEquals(oldUserEmail, expectedUser.getEmail());
        assertEquals(currentUser.getAccount(), expectedUser.getAccount());
        assertEquals(currentUser.getFriends(), expectedUser.getFriends());
    }

    @Test
    @DisplayName("should update only email")
    void shouldUpdateOnlyEmail() {
        //given
        final String oldUserEmail = "oldEmail@email.com";
        final String newUserEmail = "newEmail@email.com";
        final String oldPassword = "oldHashedPassword";
        final String newPassword = null;
        final String oldUsername = "oldUsername";
        final String newUsername = null;

        final InformationsToUpdate informationsToUpdate = new InformationsToUpdate();
        informationsToUpdate.setEmail(newUserEmail);
        informationsToUpdate.setPassword(newPassword);
        informationsToUpdate.setUsername(newUsername);

        final AppUser currentUser = AppUser.builder()
                .email(oldUserEmail)
                .username(oldUsername)
                .role(Role.USER)
                .password(oldPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        given(appUserRepository.findByEmail(oldUserEmail)).willReturn(Optional.of(currentUser));

        //when
        userService.updateUserProfil(oldUserEmail,informationsToUpdate);
        verify(appUserRepository, times(1)).save(userArgumentCaptor.capture());
        final AppUser expectedUser = userArgumentCaptor.getValue();
        assertEquals(oldUsername, expectedUser.getUsername());
        assertEquals(oldPassword, expectedUser.getPassword());
        assertEquals(newUserEmail, expectedUser.getEmail());
        assertEquals(currentUser.getAccount(), expectedUser.getAccount());
        assertEquals(currentUser.getFriends(), expectedUser.getFriends());
    }

    @Test
    @DisplayName("should update only password")
    void shouldUpdateOnlyPassword() {
        //given
        final String oldUserEmail = "oldEmail@email.com";
        final String newUserEmail = null;
        final String oldPassword = "oldHashedPassword";
        final String newPassword = "newHashedPassword";
        final String oldUsername = "oldUsername";
        final String newUsername = null;

        final InformationsToUpdate informationsToUpdate = new InformationsToUpdate();
        informationsToUpdate.setEmail(newUserEmail);
        informationsToUpdate.setPassword(newPassword);
        informationsToUpdate.setUsername(newUsername);

        final AppUser currentUser = AppUser.builder()
                .email(oldUserEmail)
                .username(oldUsername)
                .role(Role.USER)
                .password(oldPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        given(appUserRepository.findByEmail(oldUserEmail)).willReturn(Optional.of(currentUser));
        given(passwordEncoder.encode(newPassword)).willReturn(newPassword);
        //when
        userService.updateUserProfil(oldUserEmail,informationsToUpdate);
        verify(appUserRepository, times(1)).save(userArgumentCaptor.capture());
        final AppUser expectedUser = userArgumentCaptor.getValue();
        assertEquals(oldUsername, expectedUser.getUsername());
        assertEquals(newPassword, expectedUser.getPassword());
        assertEquals(oldUserEmail, expectedUser.getEmail());
        assertEquals(currentUser.getAccount(), expectedUser.getAccount());
        assertEquals(currentUser.getFriends(), expectedUser.getFriends());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException if all informations are null")
    void shouldThrowIllegalArgumentExceptionIfAllInformationsAreNull() {
        //given
        final String oldUserEmail = "oldEmail@email.com";
        final String newUserEmail = null;
        final String oldPassword = "oldHashedPassword";
        final String newPassword = null;
        final String oldUsername = "oldUsername";
        final String newUsername = null;

        final InformationsToUpdate informationsToUpdate = new InformationsToUpdate();
        informationsToUpdate.setEmail(newUserEmail);
        informationsToUpdate.setPassword(newPassword);
        informationsToUpdate.setUsername(newUsername);

        final AppUser currentUser = AppUser.builder()
                .email(oldUserEmail)
                .username(oldUsername)
                .role(Role.USER)
                .password(oldPassword)
                .account(BigDecimal.TEN)
                .friends(Collections.emptySet())
                .build();
        given(appUserRepository.findByEmail(oldUserEmail)).willReturn(Optional.of(currentUser));

        //when & then
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserProfil(oldUserEmail,informationsToUpdate));
    }

}