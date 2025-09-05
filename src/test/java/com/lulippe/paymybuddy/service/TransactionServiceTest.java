package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.InsufficientFundsException;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.entities.Transaction;
import com.lulippe.paymybuddy.persistence.enums.Role;
import com.lulippe.paymybuddy.persistence.repository.TransactionRepository;
import com.lulippe.paymybuddy.transaction.model.Transfer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private UserService userService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    @Test
    @DisplayName("should return a list of transfert")
    void shouldReturnAListOfTransfert() {
        //given
        final String email = "test@test.com";
        final String test = "test";
        final String testDescription = "testDescription";
        final BigDecimal amount = BigDecimal.valueOf(10.14569);
        final String amountString = amount.setScale(2, RoundingMode.HALF_UP).toString();
        final String friend = "friend";
        final AppUser appUser = AppUser.builder()
                .email(email)
                .username(test)
                .build();

        final AppUser appUserFriend = AppUser.builder()
                .username(friend)
                .build();
        final Transaction transaction = Transaction.builder()
                .amount(amount)
                .description(testDescription)
                .sender(appUser)
                .receiver(appUserFriend)
                .build();
        final List<Transaction> transactions = Collections.singletonList(transaction);
        given(userService.getAppUserByEmail(email)).willReturn(appUser);
        given(transactionRepository.findAllBySender(appUser)).willReturn(transactions);

        //when
        List<Transfer> transfers = transactionService.getUserSentTransactionList(email);


        //then
        assertEquals(1, transfers.size());
        assertEquals(amountString, transfers.get(0).getAmount());
        assertEquals(testDescription, transfers.get(0).getDescription());
        assertEquals(friend,transfers.get(0).getFriendName());
    }

    @Test
    @DisplayName("should return an empty list")
    void shouldReturnAnEmptyList() {
        //given
        final String email = "test@test.com";
        final String test = "test";
        final AppUser appUser = AppUser.builder()
                .email(email)
                .username(test)
                .build();
        final List<Transaction> transactions = Collections.emptyList();
        given(userService.getAppUserByEmail(email)).willReturn(appUser);
        given(transactionRepository.findAllBySender(appUser)).willReturn(transactions);

        //when
        List<Transfer> transfers = transactionService.getUserSentTransactionList(email);

        //then
        assertEquals(0, transfers.size());
    }

    @Test
    @DisplayName("should send money to Friend")
    void shouldSendMoneyToFriend() {
        //given
        final String userEmail = "test@email.com";
        final String friendEmail = "friend@email.com";
        final String userPassword = "hashedPassword";
        final String friendName = "friendName";
        final String descriptionTest = "test";
        final String amount = "5.00";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(friendName);
        transfer.setAmount(amount);
        transfer.setDescription(descriptionTest);

        final Set<AppUser> friendFriends = new HashSet<>();
        final AppUser friendUser = AppUser.builder()
                .username(friendName)
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
        given(userService.getAppUserByEmail(userEmail)).willReturn(currentUser);
        given(userService.getAppUserByName(friendName)).willReturn(friendUser);
        doNothing().when(userService).checkIfReceiverIsAFriend(friendUser,currentUser);
        doNothing().when(userService).saveTransactionAppUsers(currentUser,friendUser);

        //when
        transactionService.sendMoneyToFriend(transfer,userEmail);

        //then
        verify(transactionRepository,times(1)).save(transactionCaptor.capture());
        final Transaction transaction = transactionCaptor.getValue();
        assertEquals(descriptionTest, transaction.getDescription());
        assertEquals(new BigDecimal(amount), transaction.getAmount());
        assertEquals(transaction.getReceiver(), friendUser);
        assertEquals(transaction.getSender(), currentUser);
    }

    @Test
    @DisplayName("should throw InsufficientFundsException")
    void shouldThrowInsufficientFundsException() {
        //given
        final String userEmail = "test@email.com";
        final String friendEmail = "friend@email.com";
        final String userPassword = "hashedPassword";
        final String friendName = "friendName";
        final String descriptionTest = "test";
        final String amount = "15.00";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(friendName);
        transfer.setAmount(amount);
        transfer.setDescription(descriptionTest);

        final Set<AppUser> friendFriends = new HashSet<>();
        final AppUser friendUser = AppUser.builder()
                .username(friendName)
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
        given(userService.getAppUserByEmail(userEmail)).willReturn(currentUser);
        given(userService.getAppUserByName(friendName)).willReturn(friendUser);
        doNothing().when(userService).checkIfReceiverIsAFriend(friendUser,currentUser);

        //when & then
        assertThrows(InsufficientFundsException.class, () -> transactionService.sendMoneyToFriend(transfer,userEmail));
    }
}