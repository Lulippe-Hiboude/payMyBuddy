package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.api.exception.InsufficientFundsException;
import com.lulippe.paymybuddy.api.exception.NonexistentEntityException;
import com.lulippe.paymybuddy.bankTransfer.model.BankTransferRequest;
import com.lulippe.paymybuddy.mapper.AppUserMapper;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.repository.AppUserRepository;
import com.lulippe.paymybuddy.user.model.InformationsToUpdate;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import com.lulippe.paymybuddy.user.model.UserFriend;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

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
                .orElseThrow(() -> new NonexistentEntityException("User with email " + email + " does not exist"));
    }

    public void checkIfReceiverIsAFriend(final AppUser receiverAppUser, final AppUser senderAppUser) {
        final Set<AppUser> friends = senderAppUser.getFriends();

        if (!friends.contains(receiverAppUser)) {
            log.debug("Receiver {} is not a friend", receiverAppUser.getUsername());
            throw new IllegalArgumentException("Receiver " + receiverAppUser.getUsername() + " is not in your friends list");
        }
    }

    public AppUser getAppUserByName(final String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new NonexistentEntityException("User with name " + username + " does not exist"));
    }

    public void saveTransactionAppUsers(final AppUser receiver, final AppUser sender) {
        log.info("Saving users with updated transaction history");
        appUserRepository.save(sender);
        appUserRepository.save(receiver);
    }

    private void saveTransactionSystem(final AppUser system) {
        log.info("Saving transaction history of the system");
        appUserRepository.save(system);
    }

    public AppUser performBankTransfer(final AppUser user, final BankTransferRequest request) {
        final BigDecimal actualAccountBalance = user.getAccount();
        final BigDecimal amountToAdd = BigDecimal.valueOf(request.getAmount());
        final BigDecimal newBalance = actualAccountBalance.add(amountToAdd).setScale(2, RoundingMode.HALF_EVEN);
        user.setAccount(newBalance);
        return appUserRepository.save(user);
    }

    public void handleFriendAddition(final String userEmail, final String friendEmail) {
        final AppUser currentAppUser = getAppUserByEmail(userEmail);
        final AppUser friendAppUser = getAppUserByEmail(friendEmail);
        ensureFriendValidity(currentAppUser, friendAppUser);
        processAddFriendRequest(currentAppUser, friendAppUser);
    }

    private AppUser getSystemUser() {
        return appUserRepository.findBySystemAccountTrue()
                .orElseThrow(() -> new NonexistentEntityException("System not found"));
    }

    private void ensureFriendValidity(final AppUser currentAppUser, final AppUser friendAppUser) {
        if (friendAppUser.equals(currentAppUser)) {
            throw new IllegalArgumentException("You cannot add yourself as a friend! that is sad :(");
        }

        if (friendAppUser.isSystemAccount()) {
            log.error("User {} attempted to add the system account as a friend", currentAppUser.getUsername());
            throw new IllegalArgumentException("Invalid friend selection");
        }

        ensureFriendNotAlreadyAdded(currentAppUser, friendAppUser);
    }

    private void processAddFriendRequest(final AppUser currentAppUser, final AppUser friendAppUser) {
        currentAppUser.getFriends().add(friendAppUser);
        appUserRepository.save(currentAppUser);
    }

    private void ensureFriendNotAlreadyAdded(final AppUser currentAppUser, final AppUser friendAppUser) {
        if (currentAppUser.getFriends().contains(friendAppUser)) {
            throw new EntityAlreadyExistsException("Friend already added in current user friend list : " + friendAppUser.getEmail());
        }
    }

    public void handleSystemAccount(final BigDecimal commission) {
        final AppUser system = getSystemUser();
        system.setAccount(system.getAccount().add(commission));
        saveTransactionSystem(system);
    }

    public List<UserFriend> getAllUserFriend(final String userEmail) {
        final AppUser currentUser = getAppUserByEmail(userEmail);
        return  AppUserMapper.INSTANCE.toUserFriendList(currentUser.getFriends());
    }

    public void withdrawToBank(final AppUser user, final BigDecimal amountToWithdraw) {

        checkSufficientFunds(user.getAccount(),amountToWithdraw);
        user.setAccount(user.getAccount().subtract(amountToWithdraw));
        appUserRepository.save(user);
    }

    private void checkSufficientFunds(final BigDecimal accountFund, final BigDecimal amountToTransfer) {
        if (accountFund.compareTo(amountToTransfer) < 0) {
            throw new InsufficientFundsException("insufficient funds for transfer");
        }
    }

    public void updateUserProfil(final String userEmail, final InformationsToUpdate informationsToUpdate) {
        final AppUser currentUser = getAppUserByEmail(userEmail);
        validateInformationsToUpdate(informationsToUpdate);
        ensureUsernameAndEmailAreUniqueForUpdate(informationsToUpdate);
        if (informationsToUpdate.getPassword() != null) {
            informationsToUpdate.setPassword(passwordEncoder.encode(informationsToUpdate.getPassword()));
        }
        updateUserInformation(currentUser,informationsToUpdate);
    }

    private void ensureUsernameAndEmailAreUniqueForUpdate(final InformationsToUpdate informationsToUpdate) {
        if (informationsToUpdate.getUsername() != null || informationsToUpdate.getEmail() != null) {
            ensureUsernameAndEmailAreUnique(informationsToUpdate.getUsername(), informationsToUpdate.getEmail());
        }
    }

    private void updateUserInformation(final AppUser currentUser, final InformationsToUpdate informationsToUpdate) {
        appUserRepository.save(AppUserMapper.INSTANCE.updateUserInformation(currentUser,informationsToUpdate));
    }

    private void validateInformationsToUpdate(final InformationsToUpdate informationsToUpdate) {
        if (informationsToUpdate.getEmail() != null && !StringUtils.hasText(informationsToUpdate.getEmail())) {
            throw new IllegalArgumentException("email cannot be empty");
        }

        if (informationsToUpdate.getUsername() != null && !StringUtils.hasText(informationsToUpdate.getUsername())) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        if (informationsToUpdate.getPassword() != null && !StringUtils.hasText(informationsToUpdate.getPassword())) {
            throw new IllegalArgumentException("password cannot be empty");
        }
    }
}
