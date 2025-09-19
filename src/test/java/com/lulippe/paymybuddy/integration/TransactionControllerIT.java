package com.lulippe.paymybuddy.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferRequest;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.repository.AppUserRepository;
import com.lulippe.paymybuddy.transaction.model.Transfer;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class TransactionControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("should send money to friend ")
    void shouldSendMoneyToFriend() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user1 = createUserInDB(username, email, password);

        final String username2 = "Christian Clavier";
        final String email2 = "cc@mail.com";
        final String password2 = "123";
        final AppUser user2 = createUserInDB(username2, email2, password2);

        final AppUser updateWithFriendUser1 = addFriendToUserInDB(user1, user2);

        final String iban = "FR7712739000408237965421Y19";
        final BankTransferRequest bankTransferRequest = new BankTransferRequest();
        bankTransferRequest.setBankHolder(username);
        bankTransferRequest.setAmount(100.00);
        bankTransferRequest.setIban(iban);

        final AppUser refreshUser1 = performTransferFromBank(updateWithFriendUser1,bankTransferRequest);
        final String description = "description";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(user2.getUsername());
        transfer.setAmount("20.00");
        transfer.setDescription(description);

        final BigDecimal expectedAmountUser1 = new BigDecimal("80.00");
        final BigDecimal expectedAmountUser2 = new BigDecimal("20.00");
        //when & then
        mockMvc.perform(post("/transactions/v0/me")
                .with(csrf())
                .with(user(refreshUser1.getEmail()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isOk());

        final AppUser updateUser1 = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new AssertionError("User not found in DB"));

        final AppUser updateUser2 = appUserRepository.findByUsername(username2)
                .orElseThrow(() -> new AssertionError("User not found in DB"));

        assertEquals(expectedAmountUser1,updateUser1.getAccount());
        assertEquals(expectedAmountUser2,updateUser2.getAccount());
    }

    @Test
    @DisplayName("should return Conflict if fund is not sufficient")
    void shouldReturnConflictIfFundIsNotSufficient() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user1 = createUserInDB(username, email, password);

        final String username2 = "Christian Clavier";
        final String email2 = "cc@mail.com";
        final String password2 = "123";
        final AppUser user2 = createUserInDB(username2, email2, password2);

        final AppUser updateWithFriendUser1 = addFriendToUserInDB(user1, user2);

        final String iban = "FR7712739000408237965421Y19";
        final BankTransferRequest bankTransferRequest = new BankTransferRequest();
        bankTransferRequest.setBankHolder(username);
        bankTransferRequest.setAmount(100.00);
        bankTransferRequest.setIban(iban);

        final AppUser refreshUser1 = performTransferFromBank(updateWithFriendUser1,bankTransferRequest);
        final String description = "description";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(user2.getUsername());
        transfer.setAmount("150.00");
        transfer.setDescription(description);
        final String expectedError = "Insufficient funds";
        final BigDecimal expectedAmountUser1 = new BigDecimal("100.00");
        final BigDecimal expectedAmountUser2 = new BigDecimal("0");

        //when & then
        mockMvc.perform(post("/transactions/v0/me")
                .with(csrf())
                .with(user(refreshUser1.getEmail()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isConflict())
                .andExpect(MockMvcResultMatchers.content().string(expectedError));

        final AppUser updateUser1 = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new AssertionError("User not found in DB"));

        final AppUser updateUser2 = appUserRepository.findByUsername(username2)
                .orElseThrow(() -> new AssertionError("User not found in DB"));

        assertEquals(expectedAmountUser1,updateUser1.getAccount());
        assertEquals(expectedAmountUser2,updateUser2.getAccount());

    }

    @Test
    @DisplayName("should transfer money to friend V1")
    void shouldTransferMoneyToFriendV1() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user1 = createUserInDB(username, email, password);

        final String username2 = "Christian Clavier";
        final String email2 = "cc@mail.com";
        final String password2 = "123";
        final AppUser user2 = createUserInDB(username2, email2, password2);

        final AppUser updateWithFriendUser1 = addFriendToUserInDB(user1, user2);

        final String iban = "FR7712739000408237965421Y19";
        final BankTransferRequest bankTransferRequest = new BankTransferRequest();
        bankTransferRequest.setBankHolder(username);
        bankTransferRequest.setAmount(1000.00);
        bankTransferRequest.setIban(iban);

        final AppUser refreshUser1 = performTransferFromBank(updateWithFriendUser1,bankTransferRequest);
        final String description = "description";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(user2.getUsername());
        transfer.setAmount("200.00");
        transfer.setDescription(description);

        final BigDecimal expectedAmountUser1 = new BigDecimal("799.00");
        final BigDecimal expectedAmountUser2 = new BigDecimal("200.00");
        final BigDecimal appCommission = new BigDecimal("1.00");
        mockMvc.perform(post("/transactions/v1/me")
                .with(csrf())
                .with(user(refreshUser1.getEmail()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isOk());

        final AppUser updateUser1 = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new AssertionError("User not found in DB"));

        final AppUser updateUser2 = appUserRepository.findByUsername(username2)
                .orElseThrow(() -> new AssertionError("User not found in DB"));

        assertEquals(expectedAmountUser1,updateUser1.getAccount());
        assertEquals(expectedAmountUser2,updateUser2.getAccount());

        final AppUser system = appUserRepository.findByUsername("PLATFORM")
                .orElseThrow(() -> new AssertionError("User not found in DB"));
        assertEquals(appCommission,system.getAccount());
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

    private AppUser addFriendToUserInDB(final AppUser user1, final AppUser user2) throws Exception {
        final String expectedJson = "User added";
        mockMvc.perform(post("/users/me/friends/v0")
                        .with(csrf())
                        .with(user(user1.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("friendEmail", user2.getEmail()))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));

        return appUserRepository.findByUsername(user1.getUsername())
                .orElseThrow(() -> new AssertionError("Expected user not found"));
    }
}
