package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.InvalidDataException;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferRequest;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferResponse;
import com.lulippe.paymybuddy.bankTransfer.model.BankWithdrawResponse;
import com.lulippe.paymybuddy.mapper.AppUserMapper;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.utils.IbanUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BankTransferService {
    private final UserService userService;

    /**
     * Performs a bank transfer from the specified user to their in-app account.
     * <p>
     * The method validates the transfer request, updates the user’s account balance,
     * and returns a response containing the updated user information.
     * </p>
     *
     * @param request the {@link BankTransferRequest} containing transfer details (IBAN, amount, bank holder)
     * @param email   the email of the user performing the transfer
     * @return a {@link BankTransferResponse} containing updated user account information
     * @throws com.lulippe.paymybuddy.api.exception.NonExistentEntityException if no user is found with the given email
     * @throws com.lulippe.paymybuddy.api.exception.InvalidDataException       if the request contains invalid data (e.g., IBAN, amount, or holder)
     */
    public BankTransferResponse performBankTransfer(final BankTransferRequest request, final String email) {
        final AppUser user = userService.getAppUserByEmail(email);
        validateRequest(request);
        final AppUser updateUser = userService.performBankTransfer(user, request);
        return AppUserMapper.INSTANCE.ToBankTransferResponse(updateUser);
    }


    private void validateRequest(BankTransferRequest request) {
        if (!IbanUtil.isIbanValid(request.getIban())) {
            log.error("Invalid IBAN");
            throw new InvalidDataException("IBAN is invalid");
        }

        if (Strings.isBlank(request.getBankHolder())) {
            log.error("Invalid bank holder");
            throw new InvalidDataException("Bank holder is invalid");
        }

        if (request.getAmount() <= 0) {
            log.error("Invalid amount");
            throw new InvalidDataException("Amount is invalid, amount must be greater than zero");
        }
    }

    /**
     * Performs a withdrawal from the user’s in-app account to their bank account.
     * <p>
     * The method validates the withdrawal request, checks the available funds,
     * debits the user’s account, and returns a response containing withdrawal details.
     * </p>
     *
     * @param request the {@link BankTransferRequest} containing withdrawal details (IBAN, amount, bank holder)
     * @param email   the email of the user performing the withdrawal
     * @return a {@link BankWithdrawResponse} containing updated balance, withdrawn amount, and username
     * @throws com.lulippe.paymybuddy.api.exception.NonExistentEntityException if no user is found with the given email
     * @throws com.lulippe.paymybuddy.api.exception.InvalidDataException       if the request contains invalid data (e.g., IBAN, amount, or holder)
     * @throws com.lulippe.paymybuddy.api.exception.InsufficientFundsException if the user does not have enough balance for the withdrawal
     */
    public BankWithdrawResponse performTransferToBank(final BankTransferRequest request, final String email) {
        final AppUser user = userService.getAppUserByEmail(email);
        validateRequest(request);
        final BigDecimal amountToWithdraw = BigDecimal.valueOf(request.getAmount()).setScale(2, RoundingMode.HALF_EVEN);
        userService.withdrawToBank(user, amountToWithdraw);
        return AppUserMapper.INSTANCE.toBankWithdrawResponse(user.getAccount(), amountToWithdraw, user.getUsername());
    }
}
