package com.lulippe.paymybuddy.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulippe.paymybuddy.api.exception.InsufficientFundsException;
import com.lulippe.paymybuddy.api.exception.NonexistentEntityException;
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

import java.util.Collections;
import java.util.List;

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
        doThrow(new NonexistentEntityException("User with email " + email + " does not exist")).when(transactionService).getUserSentTransactionList(email);

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
    @DisplayName("should return Bad Request")
    void shouldReturnBadRequest() throws Exception {
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
}