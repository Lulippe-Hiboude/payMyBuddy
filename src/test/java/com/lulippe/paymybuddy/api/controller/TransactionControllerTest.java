package com.lulippe.paymybuddy.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulippe.paymybuddy.api.exception.InsufficientFundsException;
import com.lulippe.paymybuddy.api.exception.NonExistentEntityException;
import com.lulippe.paymybuddy.service.TransactionService;
import com.lulippe.paymybuddy.transaction.model.Transfer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc
class TransactionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return a list of transfers")
    void shouldReturnListOfTransfers() throws Exception {
        //given
        final String email = "test@test.com";
        final String friend = "friend";
        final String amount = "10.00";
        final String description = "description";

        final Transfer transfer = new Transfer();
        transfer.setFriendName(friend);
        transfer.setAmount(amount);
        transfer.setDescription(description);

        final List<Transfer> transfers = Collections.singletonList(transfer);
        given(transactionService.getUserSentTransactionList(email)).willReturn(transfers);

        //when
        mockMvc.perform(get("/transactions/v0/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].friendName").value(friend))
                .andExpect(jsonPath("$[0].amount").value(amount))
                .andExpect(jsonPath("$[0].description").value(description));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should throw EntityNotFoundException")
    void shouldThrowEntityNotFoundException() throws Exception {
        //given
        final String email = "test@test.com";
        doThrow(new NonExistentEntityException("User with email " + email + " does not exist")).when(transactionService).getUserSentTransactionList(email);

        //when & then
        mockMvc.perform(get("/transactions/v0/me"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User with email test@test.com does not exist"));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should transfer money to friend")
    void shouldTransferMoneyToFriend() throws Exception {
        //given
        final String email = "test@test.com";
        final String friendName= "friend";
        final String amount = "10.00";
        final String description = "description";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(friendName);
        transfer.setAmount(amount);
        transfer.setDescription(description);
        given(transactionService.sendMoneyToFriend(transfer,email)).willReturn("Transfer of "+10.00+ " from test to " + friendName+ " completed successfully.");

        //when & then
        mockMvc.perform(post("/transactions/v0/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return Conflict if fund is not sufficient")
    void shouldReturnConflictIfFundIsNotSufficient() throws Exception {
        //given
        final String email = "test@test.com";
        final String friendName= "friend";
        final String amount = "10.00";
        final String description = "description";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(friendName);
        transfer.setAmount(amount);
        transfer.setDescription(description);
        doThrow(new InsufficientFundsException("insufficient funds")).when(transactionService).sendMoneyToFriend(transfer,email);

        //when & then
        mockMvc.perform(post("/transactions/v0/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isConflict());
    }
    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return Bad Request when friend not in friend list")
    void shouldReturnBadRequestWhenFriendNotInFriendList() throws Exception {
        //given
        final String email = "test@test.com";
        final String friendName= "friend";
        final String amount = "10.00";
        final String description = "description";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(friendName);
        transfer.setAmount(amount);
        transfer.setDescription(description);
        doThrow(new IllegalArgumentException("Receiver is not in your friends list")).when(transactionService).sendMoneyToFriend(transfer,email);
        //when & then
        mockMvc.perform(post("/transactions/v0/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should transfer money to friend V1")
    void shouldTransferMoneyToFriendV1() throws Exception {
        //given
        final String email = "test@test.com";
        final String friendName= "friend";
        final String amount = "5.00";
        final String description = "description";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(friendName);
        transfer.setAmount(amount);
        transfer.setDescription(description);
        final BigDecimal commission = BigDecimal.valueOf(0.02);
        final String response = "Transfer of " + amount + " € from " + email + " to " + friendName + " completed successfully. A commission of " + commission + " € has been deducted from your account";
        given(transactionService.sendMoneyToFriendV1(transfer,email)).willReturn(response);

        //when & then
        mockMvc.perform(post("/transactions/v1/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return Conflict if fund is not sufficient V1")
    void shouldReturnConflictIfFundIsNotSufficientV1() throws Exception {
        //given
        final String email = "test@test.com";
        final String friendName= "friend";
        final String amount = "10.00";
        final String description = "description";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(friendName);
        transfer.setAmount(amount);
        transfer.setDescription(description);
        doThrow(new InsufficientFundsException("insufficient funds")).when(transactionService).sendMoneyToFriendV1(transfer,email);

        //when & then
        mockMvc.perform(post("/transactions/v1/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return Bad Request when transfer is invalid")
    void shouldReturnBadRequestWhenTransferIsInvalid() throws Exception {
        String invalidJson = """
            {
              "friendName": "Jean",
              "amount": ""
            }
            """;

        mockMvc.perform(post("/transactions/v1/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("amount")));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return Bad Request when friend not in friend list V1")
    void shouldReturnBadRequestWhenFriendNotInFriendListV1() throws Exception {
        //given
        final String email = "test@test.com";
        final String friendName= "friend";
        final String amount = "10.00";
        final String description = "description";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(friendName);
        transfer.setAmount(amount);
        transfer.setDescription(description);
        doThrow(new IllegalArgumentException("Receiver is not in your friends list")).when(transactionService).sendMoneyToFriendV1(transfer,email);
        //when & then
        mockMvc.perform(post("/transactions/v1/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return Bad Request when amount is negative")
    void shouldReturnBadRequestWhenAmountIsNegative() throws Exception {
        //given
        final String email = "test@test.com";
        final String friendName= "friend";
        final String amount = "-10.00";
        final String description = "description";
        final Transfer transfer = new Transfer();
        transfer.setFriendName(friendName);
        transfer.setAmount(amount);
        transfer.setDescription(description);
        doThrow(new IllegalArgumentException("Transfer amount must be greater than zero")).when(transactionService).sendMoneyToFriendV1(transfer,email);

        //when & then
        mockMvc.perform(post("/transactions/v1/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isBadRequest());
    }
}