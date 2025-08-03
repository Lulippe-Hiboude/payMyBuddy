package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.mapper.TransactionMapper;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.entities.Transaction;
import com.lulippe.paymybuddy.persistence.repository.TransactionRepository;
import com.lulippe.paymybuddy.transaction.model.Transfer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionService {
    private final UserService userService;
    private final TransactionRepository transactionRepository;

    public List<Transfer> getUserSentTransactionList(final String email) {
        final AppUser appUser = userService.getAppUserByEmail(email);
        final List<Transaction> transactions = getUserTransactionList(appUser);

        return transactions.stream()
                .map(TransactionMapper.INSTANCE::toTransfert)
                .collect(Collectors.toList());
    }

    private List<Transaction> getUserTransactionList(final AppUser appUser) {
        return transactionRepository.findAllBySender(appUser);
    }
}
