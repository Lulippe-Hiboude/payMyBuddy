package com.lulippe.paymybuddy.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulippe.paymybuddy.api.exception.InvalidDataException;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferRequest;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferResponse;
import com.lulippe.paymybuddy.service.BankTransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BankTransferController.class)
@AutoConfigureMockMvc
class BankTransferControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BankTransferService bankTransferService;

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return a BankTransferResponse")
    void shouldReturnBankTransferResponse() throws Exception {
        //given
        final String email = "test@test.com";
        final String iban = "FR7712739000408237965421Y19";
        final String testBankHolder = "testBankHolder";
        final double amount = 20.126;
        final BankTransferRequest request = new BankTransferRequest();
        request.setAmount(amount);
        request.setBankHolder(testBankHolder);
        request.setIban(iban);
        final String username = "testUsername";
        final String receivedAmount = "30.13";
        final BankTransferResponse bankTransferResponse = new BankTransferResponse();
        bankTransferResponse.setReceiver(username);
        bankTransferResponse.setAmount(receivedAmount);
        given(bankTransferService.performBankTransfer(request, email)).willReturn(bankTransferResponse);

        //when & then
        mockMvc.perform(post("/transfer-from-bank/v0")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiver").value(username))
                .andExpect(jsonPath("$.amount").value(receivedAmount));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return InvalidDataException if iban is invalid")
    void shouldReturnInvalidDataExceptionIfIbanIsInvalid() throws Exception {
        //given
        final String email = "test@test.com";
        final String iban = "invalidIban";
        final String testBankHolder = "testBankHolder";
        final double amount = 20.126;
        final BankTransferRequest request = new BankTransferRequest();
        request.setAmount(amount);
        request.setBankHolder(testBankHolder);
        request.setIban(iban);

        doThrow(new InvalidDataException("invalidIban")).when(bankTransferService).performBankTransfer(request, email);


        //when & then
        mockMvc.perform(post("/transfer-from-bank/v0")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }
}