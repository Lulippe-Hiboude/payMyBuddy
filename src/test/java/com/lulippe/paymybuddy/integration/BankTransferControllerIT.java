package com.lulippe.paymybuddy.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferRequest;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class BankTransferControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("should add money to the user account")
    void shouldAddMoneyToTheUserAccount() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user = createUserInDB(username, email, password);
        final String iban = "FR7712739000408237965421Y19";
        final String receivedAmount = "100.00";
        final BigDecimal amount = new BigDecimal(receivedAmount);
        final BankTransferRequest bankTransferRequest = new BankTransferRequest();
        bankTransferRequest.setBankHolder(username);
        bankTransferRequest.setAmount(100.00);
        bankTransferRequest.setIban(iban);

        //when & then
        mockMvc.perform(post("/transfer-from-bank/v0")
                        .with(csrf())
                        .with(user(user.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bankTransferRequest))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiver").value(username))
                .andExpect(jsonPath("$.amount").value(receivedAmount));

        final AppUser refreshUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new AssertionError("Expected user not found"));
        assertEquals(amount, refreshUser.getAccount());
    }

    @Test
    @DisplayName("should throw InvalidDataException if iban is invalid")
    void shouldThrowInvalidDataExceptionIfIbanIsInvalid() throws Exception {
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final String iban = "invalidIBAN";
        final AppUser user = createUserInDB(username, email, password);
        final BankTransferRequest bankTransferRequest = new BankTransferRequest();
        bankTransferRequest.setBankHolder(username);
        bankTransferRequest.setAmount(100.00);
        bankTransferRequest.setIban(iban);
        final String expectedError = "IBAN is invalid";
        //when & then

        mockMvc.perform(post("/transfer-from-bank/v0")
                        .with(csrf())
                        .with(user(user.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bankTransferRequest))
                )
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().string(expectedError));
    }

    @Test
    @DisplayName("should perform TransferToBank")
    void shouldPerformTransferToBank() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user = createUserInDB(username, email, password);
        final String iban = "FR7712739000408237965421Y19";
        final BankTransferRequest bankTransferRequest = new BankTransferRequest();
        bankTransferRequest.setBankHolder(username);
        bankTransferRequest.setAmount(100.00);
        bankTransferRequest.setIban(iban);
        final AppUser refreshUser = performTransferFromBank(user,bankTransferRequest);

        final BankTransferRequest request = new BankTransferRequest();
        request.setAmount(50.00);
        request.setBankHolder(username);
        request.setIban(iban);

        final String receivedAmount = "50.00";
        //when & then
        mockMvc.perform(post("/transfer-to-bank/v0")
                        .with(csrf())
                        .with(user(user.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiver").value(username))
                .andExpect(jsonPath("$.amount").value(receivedAmount))
                .andExpect(jsonPath("$.newBalance").value(receivedAmount));
    }

    @Test
    @DisplayName("should throw InsufficientFundsException")
    void shouldThrowInsufficientFundsException() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user = createUserInDB(username, email, password);
        final String iban = "FR7712739000408237965421Y19";
        final BankTransferRequest bankTransferRequest = new BankTransferRequest();
        bankTransferRequest.setBankHolder(username);
        bankTransferRequest.setAmount(100.00);
        bankTransferRequest.setIban(iban);
        final AppUser refreshUser = performTransferFromBank(user,bankTransferRequest);

        final BankTransferRequest request = new BankTransferRequest();
        request.setAmount(150.00);
        request.setBankHolder(username);
        request.setIban(iban);

        final String expectedError = "insufficient funds for transfer";
        //when & then
        mockMvc.perform(post("/transfer-to-bank/v0")
                        .with(csrf())
                        .with(user(user.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(MockMvcResultMatchers.content().string(expectedError));
    }

    private AppUser createUserInDB(final String username, final String email, final String password) throws Exception {
        final String expectedJson = "user registered successfully";
        final RegisterRequest.RoleEnum role = RegisterRequest.RoleEnum.USER;
        final RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        registerRequest.setRole(role);
        mockMvc.perform(post("/auth/register/v0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string(expectedJson));

        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new AssertionError("User not found in DB"));
    }

    private AppUser performTransferFromBank(final AppUser appUser,final BankTransferRequest bankTransferRequest) throws Exception {
        mockMvc.perform(post("/transfer-from-bank/v0")
                        .with(csrf())
                        .with(user(appUser.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bankTransferRequest))
                )
                .andExpect(status().isOk());

        return appUserRepository.findByUsername(appUser.getUsername())
                .orElseThrow(() -> new AssertionError("Expected user not found"));
    }
}
