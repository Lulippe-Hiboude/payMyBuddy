package com.lulippe.paymybuddy.api.controller;

import com.lulippe.paymybuddy.service.TransactionService;
import com.lulippe.paymybuddy.transaction.api.TransactionsApi;
import com.lulippe.paymybuddy.transaction.model.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.lulippe.paymybuddy.utils.AuthenticationUtil.getAuthenticatedUserEmail;

@Slf4j
@RestController
@RequiredArgsConstructor

public class TransactionController implements TransactionsApi {
    private final TransactionService transactionService;

    @Override
    public ResponseEntity<List<Transfer>> getUserTransactionList() {
        final String email = getAuthenticatedUserEmail();
        log.info("Fetching transactions for user: {}", email);
        final List<Transfer> transactions = transactionService.getUserSentTransactionList(email);
        return ResponseEntity.ok(transactions);
    }

    @Override
    public ResponseEntity<String> transferToFriend(final Transfer transfer) {
        final String email = getAuthenticatedUserEmail();
        final String response = transactionService.sendMoneyToFriend(transfer, email);
        return ResponseEntity.ok(response);
    }
}
