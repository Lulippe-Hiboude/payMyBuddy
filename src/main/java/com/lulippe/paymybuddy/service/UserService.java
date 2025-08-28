package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.api.exception.InexistantEntityException;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferRequest;
import com.lulippe.paymybuddy.mapper.AppUserMapper;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.repository.AppUserRepository;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {
    private final AppUserRepository appUserRepository;

    public void ensureUsernameAndEmailAreUnique(final String username, final String email) {
        if (appUserRepository.existsByUsernameOrEmail(username, email)) {
            throw new EntityAlreadyExistsException("A user with this email or username already exists");
        }
    }

    public void createAppUser(final String username, final String email, final String hashedPassword, final RegisterRequest.RoleEnum role) {
        final AppUser appUser = AppUserMapper.INSTANCE.ToAppUser(username, email, hashedPassword, role);
        log.debug("AppUser to save: {}", appUser);
        appUserRepository.save(appUser);
        log.debug("Created Information for AppUser: {}", appUser);
    }

    public AppUser getAppUserByEmail(final String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new InexistantEntityException("User with email " + email + " does not exist"));
    }

    public AppUser performBankTransfer(final AppUser user, final BankTransferRequest request) {
        final BigDecimal actualAccountBalance = user.getAccount();
        final BigDecimal amountToAdd = BigDecimal.valueOf(request.getAmount());
        final BigDecimal newBalance = actualAccountBalance.add(amountToAdd).setScale(2, RoundingMode.HALF_EVEN);
        user.setAccount(newBalance);
        return appUserRepository.save(user);
    }
}
