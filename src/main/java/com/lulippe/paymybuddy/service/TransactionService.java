package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.InsufficientFundsException;
import com.lulippe.paymybuddy.api.exception.NonExistentEntityException;
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
import java.math.RoundingMode;
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

    /**
     * Retrieves the list of transactions sent by a given user.
     *
     * @param email the email of the user whose sent transactions should be retrieved
     * @return a list of {@link Transfer} objects representing the transactions sent by the user
     * @throws NonExistentEntityException if no user is found with the provided email
     */
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

    /**
     * Sends money from the current user to a friend without commission.
     *
     * @param transfer  the {@link Transfer} object containing the recipient and transfer details
     * @param userEmail the email of the sender user
     * @return a confirmation message describing the transfer
     * @throws com.lulippe.paymybuddy.api.exception.NonExistentEntityException if either the sender or receiver user does not exist
     * @throws IllegalArgumentException                                        if the receiver is not in the sender's friends list
     * @throws InsufficientFundsException                                      if the sender has insufficient funds
     */
    public String sendMoneyToFriend(final Transfer transfer, final String userEmail) {
        final AppUser senderAppUser = userService.getAppUserByEmail(userEmail);
        final AppUser receiverAppUser = userService.getAppUserByName(transfer.getFriendName());
        userService.checkIfReceiverIsAFriend(receiverAppUser, senderAppUser);
        final BigDecimal transferAmount = stringToBigDecimal(transfer.getAmount());
        checkPositiveTransfertAmount(transferAmount);
        checkSufficientFunds(transferAmount, senderAppUser.getAccount());
        return processMoneyTransfer(senderAppUser, receiverAppUser, transferAmount, transfer.getDescription());
    }

    /**
     * Sends money from the current user to a friend with a commission applied.
     * <p>
     * The commission is calculated as 0.5% of the transfer amount and is credited to the system account.
     * </p>
     *
     * @param transfer  the {@link Transfer} object containing the recipient and transfer details
     * @param userEmail the email of the sender user
     * @return a confirmation message describing the transfer, including commission details
     * @throws com.lulippe.paymybuddy.api.exception.NonExistentEntityException if either the sender or receiver user does not exist
     * @throws IllegalArgumentException                                        if the receiver is not in the sender's friends list
     * @throws InsufficientFundsException                                      if the sender has insufficient funds for both the transfer and the commission
     */
    public String sendMoneyToFriendV1(final Transfer transfer, final String userEmail) {
        final AppUser senderAppUser = userService.getAppUserByEmail(userEmail);
        final AppUser receiverAppUser = userService.getAppUserByName(transfer.getFriendName());
        userService.checkIfReceiverIsAFriend(receiverAppUser, senderAppUser);
        final BigDecimal transferAmount = stringToBigDecimal(transfer.getAmount());
        checkPositiveTransfertAmount(transferAmount);
        final BigDecimal commission = transferAmount.multiply(BigDecimal.valueOf(0.005)).setScale(2, RoundingMode.HALF_EVEN);
        final BigDecimal totalDebit = transferAmount.add(commission);
        checkSufficientFunds(totalDebit, senderAppUser.getAccount());
        return processMoneyTransferV1(senderAppUser, receiverAppUser, transferAmount, commission, totalDebit, transfer.getDescription());
    }

    private static void checkPositiveTransfertAmount(final BigDecimal transferAmount) {
        if(transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Transfer amount must be greater than zero : {}", transferAmount);
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
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

        final Transaction transaction = TransactionMapper.INSTANCE.toTransaction(senderAppUser, receiverAppUser, transferAmount, description, Instant.now());

        transactionRepository.save(transaction);
        userService.saveTransactionAppUsers(senderAppUser, receiverAppUser);
        log.info("Transfer of {} from {} to {} completed successfully.", transferAmount, senderAppUser.getUsername(), receiverAppUser.getUsername());
        return "Transfer of " + transferAmount + " from " + senderAppUser.getUsername() + " to " + receiverAppUser.getUsername() + " completed successfully.";
    }

    private String processMoneyTransferV1(final AppUser senderAppUser, final AppUser receiverAppUser, final BigDecimal transferAmount, final BigDecimal commission, final BigDecimal totalDebit, final String description) {
        senderAppUser.setAccount(senderAppUser.getAccount().subtract(totalDebit));
        receiverAppUser.setAccount(receiverAppUser.getAccount().add(transferAmount));
        userService.handleSystemAccount(commission);

        final Transaction transaction = TransactionMapper.INSTANCE.toTransaction(senderAppUser, receiverAppUser, transferAmount, description, Instant.now());
        transactionRepository.save(transaction);
        userService.saveTransactionAppUsers(senderAppUser, receiverAppUser);

        log.info("Transfer of {} € from {} to {} completed successfully. Commission for app is {} €", transferAmount, senderAppUser.getUsername(), receiverAppUser.getUsername(), commission);
        return "Transfer of " + transferAmount + " € from " + senderAppUser.getUsername() + " to " + receiverAppUser.getUsername() + " completed successfully. A commission of " + commission + " € has been deducted from your account";
    }
}
