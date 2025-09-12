package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.InvalidDataException;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferRequest;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferResponse;
import com.lulippe.paymybuddy.bankTransfer.model.BankWithdrawResponse;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class BankTransferServiceTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private BankTransferService bankTransferService;

    @Test
    @DisplayName("should perform bank transfer")
    void bankTransfer() {
        //given
        final String email = "test@email.com";
        final String iban = "FR7712739000408237965421Y19";
        final BankTransferRequest request = new BankTransferRequest();
        request.setAmount(20.126);
        request.setBankHolder("testBankHolder");
        request.setIban(iban);
        final String username = "testUsername";
        final AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .role(Role.USER)
                .password("hashedPassword")
                .account(BigDecimal.TEN)
                .build();
        final AppUser updatedUser =
                AppUser.builder()
                        .username(username)
                        .email(email)
                        .role(Role.USER)
                        .password("hashedPassword")
                        .account(BigDecimal.valueOf(30.13))
                        .build();
        final BankTransferResponse bankTransferResponse = new BankTransferResponse();
        bankTransferResponse.setReceiver(username);
        bankTransferResponse.setAmount("30.13");
        given(userService.getAppUserByEmail(email)).willReturn(user);
        given(userService.performBankTransfer(user, request)).willReturn(updatedUser);

        //when
        final BankTransferResponse expectedBankTransferResponse = bankTransferService.performBankTransfer(request,email);

        //then
        assertEquals(expectedBankTransferResponse, bankTransferResponse);
    }

    @Test
    @DisplayName("should throw InvalidDataException if iban is invalid")
    void invalidBank() {
        //given
        final String email = "test@email.com";
        final String iban = "invalidIban";
        final BankTransferRequest request = new BankTransferRequest();
        request.setAmount(20.126);
        request.setBankHolder("testBankHolder");
        request.setIban(iban);
        final String username = "testUsername";
        final AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .role(Role.USER)
                .password("hashedPassword")
                .account(BigDecimal.TEN)
                .build();
        given(userService.getAppUserByEmail(email)).willReturn(user);

        //when & then
        assertThrows(InvalidDataException.class, () -> bankTransferService.performBankTransfer(request,email) );
    }

    @Test
    @DisplayName("should throw InvalidDataException if bankHolder is invalid")
    void invalidBankHolder() {
        //given
        final String email = "test@email.com";
        final String iban = "FR7712739000408237965421Y19";
        final String testBankHolder = "  ";
        final double amount = 20.126;
        final BankTransferRequest request = new BankTransferRequest();
        request.setAmount(amount);
        request.setBankHolder(testBankHolder);
        request.setIban(iban);
        final String username = "testUsername";
        final AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .role(Role.USER)
                .password("hashedPassword")
                .account(BigDecimal.TEN)
                .build();
        given(userService.getAppUserByEmail(email)).willReturn(user);

        //when & then
        assertThrows(InvalidDataException.class, () -> bankTransferService.performBankTransfer(request,email) );
    }

    @Test
    @DisplayName("should throw InvalidDataException if the amount is negative")
    void invalidAmount() {
        //given
        final String email = "test@email.com";
        final String iban = "FR7712739000408237965421Y19";
        final String testBankHolder = "testBankHolder";
        final double amount = -20.126;
        final BankTransferRequest request = new BankTransferRequest();
        request.setAmount(amount);
        request.setBankHolder(testBankHolder);
        request.setIban(iban);
        final String username = "testUsername";
        final AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .role(Role.USER)
                .password("hashedPassword")
                .account(BigDecimal.TEN)
                .build();
        given(userService.getAppUserByEmail(email)).willReturn(user);
        //when & then
        assertThrows(InvalidDataException.class, () -> bankTransferService.performBankTransfer(request,email) );
    }

    @Test
    @DisplayName("should perform transfer from app to bank")
    void transferFromApp() {
        //given
        final String email = "test@email.com";
        final String iban = "FR7712739000408237965421Y19";
        final double amount = 5.0;
        final String testUser = "testUser";
        final BankTransferRequest request = new BankTransferRequest();
        request.setAmount(amount);
        request.setBankHolder(testUser);
        request.setIban(iban);
        final AppUser user = AppUser.builder()
                .username(testUser)
                .friends(Collections.emptySet())
                .email(email)
                .role(Role.USER)
                .password("hashedPassword")
                .account(BigDecimal.TEN)
                .build();
        final BigDecimal amountToWithdraw = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_EVEN);
        given(userService.getAppUserByEmail(email)).willReturn(user);
        doAnswer(invocation -> {
                AppUser u = invocation.getArgument(0);
                BigDecimal amountBD = invocation.getArgument(1);
                u.setAccount(u.getAccount().subtract(amountBD));
                return null;
        }).when(userService).withdrawToBank(user,amountToWithdraw);

        //when
        final BankWithdrawResponse response = bankTransferService.performTransferToBank(request,email);
        assertEquals(BigDecimal.valueOf(amount).setScale(2,RoundingMode.HALF_EVEN), new BigDecimal(response.getAmount()));
        assertEquals(testUser, response.getReceiver());
        assertEquals(String.valueOf(amountToWithdraw), response.getNewBalance());
    }
}