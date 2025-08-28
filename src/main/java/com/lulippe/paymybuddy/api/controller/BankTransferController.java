package com.lulippe.paymybuddy.api.controller;

import com.lulippe.paymybuddy.bankTransfer.api.BankTransferApi;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferRequest;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferResponse;
import com.lulippe.paymybuddy.service.BankTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BankTransferController implements BankTransferApi {

    private final BankTransferService bankTransferService;

    @Override
    public ResponseEntity<BankTransferResponse> transferFromBank(final BankTransferRequest bankTransferRequest) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final String email = authentication.getName();
        log.info("start bank transfer for user {}", email);
        final BankTransferResponse bankTransferResponse = bankTransferService.performBankTransfer(bankTransferRequest, email);
        return ResponseEntity.ok(bankTransferResponse);
    }
}
