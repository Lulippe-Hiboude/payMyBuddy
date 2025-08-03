package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.entities.Transaction;
import com.lulippe.paymybuddy.persistence.repository.TransactionRepository;
import com.lulippe.paymybuddy.transaction.model.Transfer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private UserService userService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

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
}