package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.InsufficientFundsException;
import com.lulippe.paymybuddy.mapper.TransactionMapper;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.entities.Transaction;
import com.lulippe.paymybuddy.persistence.repository.TransactionRepository;
import com.lulippe.paymybuddy.transaction.model.Transfer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static com.lulippe.paymybuddy.utils.BigDecimalUtils.stringToBigDecimal;

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

    public String sendMoneyToFriend(final Transfer transfer, final String userEmail) {
        final AppUser senderAppUser = userService.getAppUserByEmail(userEmail);
        final AppUser receiverAppUser = userService.getAppUserByName(transfer.getFriendName());
        userService.checkIfReceiverIsAFriend(receiverAppUser, senderAppUser);
        final BigDecimal transferAmount = stringToBigDecimal(transfer.getAmount());
        checkSufficientFunds(transferAmount, senderAppUser.getAccount());
        return processMoneyTransfer(senderAppUser, receiverAppUser, transferAmount, transfer.getDescription());
    }

    private void checkSufficientFunds(final BigDecimal transferAmount, final BigDecimal currentBalance) {
        if (currentBalance.compareTo(transferAmount) < 0) {
            log.error("Insufficient funds!");
            throw new InsufficientFundsException("Insufficient funds");
        }
    }

    private String processMoneyTransfer(final AppUser senderAppUser, final AppUser receiverAppUser, final BigDecimal transferAmount, final String description) {
        senderAppUser.setAccount(senderAppUser.getAccount().subtract(transferAmount));
        receiverAppUser.setAccount(receiverAppUser.getAccount().add(transferAmount));

        final Transaction transaction = TransactionMapper.INSTANCE.toTransaction(senderAppUser,receiverAppUser,transferAmount,description,Instant.now());

        transactionRepository.save(transaction);
        userService.saveTransactionAppUsers(senderAppUser, receiverAppUser);
        log.info("Transfer of {} from {} to {} completed successfully.", transferAmount, senderAppUser.getUsername(), receiverAppUser.getUsername());
        return "Transfer of " + transferAmount + " from " + senderAppUser.getUsername() + " to " + receiverAppUser.getUsername() + " completed successfully.";
    }
}
