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

    public BankWithdrawResponse performTransferToBank(final BankTransferRequest request, final String email) {
        final AppUser user = userService.getAppUserByEmail(email);
        validateRequest(request);
        final BigDecimal amountToWithdraw = BigDecimal.valueOf(request.getAmount()).setScale(2, RoundingMode.HALF_EVEN);
        userService.withdrawToBank(user, amountToWithdraw);
        return AppUserMapper.INSTANCE.toBankWithdrawResponse(user.getAccount(), amountToWithdraw, user.getUsername());
    }
}
