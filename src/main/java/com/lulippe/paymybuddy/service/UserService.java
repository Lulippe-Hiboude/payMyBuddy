package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.api.exception.InsufficientFundsException;
import com.lulippe.paymybuddy.api.exception.NonExistentEntityException;
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
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
/**
 * Service class for managing application users, their friends and accounts.
 * Provides methods to handle user registration, profile updates and friend management.
 */
public class UserService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Ensures that the provided username and email are unique across the system.
     *
     * @param username the username to check
     * @param email    the email to check
     * @throws EntityAlreadyExistsException if a user with the same username or email already exists
     */
    public void ensureUsernameAndEmailAreUnique(final String username, final String email) {
        if (appUserRepository.existsByUsernameOrEmail(username, email)) {
            throw new EntityAlreadyExistsException("A user with this email or username already exists");
        }
    }

    /**
     * Creates and persists a new application user.
     *
     * @param username       the username of the new user
     * @param email          the email of the new user
     * @param hashedPassword the hashed password of the new user
     * @param role           the role of the new user
     */
    public void createAppUser(final String username, final String email, final String hashedPassword, final RegisterRequest.RoleEnum role) {
        final AppUser appUser = AppUserMapper.INSTANCE.ToAppUser(username, email, hashedPassword, role);
        log.debug("AppUser to save: {}", appUser);
        appUserRepository.save(appUser);
        log.debug("Created Information for AppUser: {}", appUser);
    }

    /**
     * Retrieves an application user by their email.
     *
     * @param email the email of the user
     * @return the corresponding {@link AppUser}
     * @throws NonExistentEntityException if no user is found with the given email
     */
    public AppUser getAppUserByEmail(final String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new NonExistentEntityException("User with email " + email + " does not exist"));
    }

    /**
     * Checks whether the receiver user is in the sender's friend list.
     *
     * @param receiverAppUser the user receiving the transfer
     * @param senderAppUser   the user sending the transfer
     * @throws IllegalArgumentException if the receiver is not in the sender's friend list
     */
    public void checkIfReceiverIsAFriend(final AppUser receiverAppUser, final AppUser senderAppUser) {
        final Set<AppUser> friends = senderAppUser.getFriends();

        if (!friends.contains(receiverAppUser)) {
            log.debug("Receiver {} is not a friend", receiverAppUser.getUsername());
            throw new IllegalArgumentException("Receiver " + receiverAppUser.getUsername() + " is not in your friends list");
        }
    }

    /**
     * Retrieves an application user by their username.
     *
     * @param username the username of the user
     * @return the corresponding {@link AppUser}
     * @throws NonExistentEntityException if no user is found with the given username
     */
    public AppUser getAppUserByName(final String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new NonExistentEntityException("User with name " + username + " does not exist"));
    }

    /**
     * Persists both sender and receiver after a transaction.
     *
     * @param receiver the receiving user
     * @param sender   the sending user
     */
    public void saveTransactionAppUsers(final AppUser receiver, final AppUser sender) {
        log.info("Saving users with updated transaction history");
        appUserRepository.save(sender);
        appUserRepository.save(receiver);
    }

    private void saveTransactionSystem(final AppUser system) {
        log.info("Saving transaction history of the system");
        appUserRepository.save(system);
    }

    /**
     * Performs a bank transfer operation for a user, adding the requested amount
     * to their account balance.
     *
     * @param user    the user performing the bank transfer
     * @param request the bank transfer request containing the amount to transfer
     * @return the updated {@link AppUser} after the transfer
     */
    public AppUser performBankTransfer(final AppUser user, final BankTransferRequest request) {
        final BigDecimal actualAccountBalance = user.getAccount();
        final BigDecimal amountToAdd = BigDecimal.valueOf(request.getAmount());
        final BigDecimal newBalance = actualAccountBalance.add(amountToAdd).setScale(2, RoundingMode.HALF_EVEN);
        user.setAccount(newBalance);
        return appUserRepository.save(user);
    }

    /**
     * Handles the addition of a friend to the current user's friend list.
     *
     * @param userEmail   the email of the current authenticated user
     * @param friendEmail the email of the friend to add
     * @throws NonExistentEntityException   if either user does not exist
     * @throws EntityAlreadyExistsException if the friend is already in the friend list
     * @throws IllegalArgumentException     if the friend is invalid (same user, or system account)
     */
    public void handleFriendAddition(final String userEmail, final String friendEmail) {
        final AppUser currentAppUser = getAppUserByEmail(userEmail);
        final AppUser friendAppUser = getAppUserByEmail(friendEmail);
        ensureFriendValidity(currentAppUser, friendAppUser);
        processAddFriendRequest(currentAppUser, friendAppUser);
    }

    private AppUser getSystemUser() {
        return appUserRepository.findBySystemAccountTrue()
                .orElseThrow(() -> new NonExistentEntityException("System not found"));
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

    /**
     * Handles the update of the system account by adding a commission to its balance.
     *
     * @param commission the commission amount to add to the system account
     * @throws NonExistentEntityException if the system account does not exist
     */
    public void handleSystemAccount(final BigDecimal commission) {
        final AppUser system = getSystemUser();
        system.setAccount(system.getAccount().add(commission));
        saveTransactionSystem(system);
    }

    /**
     * Retrieves all friends of a given user.
     *
     * @param userEmail the email of the current user
     * @return a list of {@link UserFriend} representing the user's friends
     * @throws NonExistentEntityException if the user does not exist
     */
    public List<UserFriend> getAllUserFriend(final String userEmail) {
        final AppUser currentUser = getAppUserByEmail(userEmail);
        return AppUserMapper.INSTANCE.toUserFriendList(currentUser.getFriends());
    }

    /**
     * Withdraws funds from a user's account to a bank account.
     *
     * @param user             the user performing the withdrawal
     * @param amountToWithdraw the amount to withdraw
     * @throws InsufficientFundsException if the user has insufficient funds
     */
    public void withdrawToBank(final AppUser user, final BigDecimal amountToWithdraw) {

        checkSufficientFunds(user.getAccount(), amountToWithdraw);
        user.setAccount(user.getAccount().subtract(amountToWithdraw));
        appUserRepository.save(user);
    }

    private void checkSufficientFunds(final BigDecimal accountFund, final BigDecimal amountToTransfer) {
        if (accountFund.compareTo(amountToTransfer) < 0) {
            throw new InsufficientFundsException("insufficient funds for transfer");
        }
    }

    /**
     * Updates the profile of a user with the provided information.
     *
     * @param userEmail            the email of the current user
     * @param informationsToUpdate the information to update (email, username, password)
     * @throws IllegalArgumentException     if no values are provided or values are invalid
     * @throws EntityAlreadyExistsException if the new username or email already exists
     * @throws NonExistentEntityException   if the user does not exist
     */
    public void updateUserProfil(final String userEmail, final InformationsToUpdate informationsToUpdate) {
        final AppUser currentUser = getAppUserByEmail(userEmail);
        validateInformationsToUpdate(informationsToUpdate);
        ensureUsernameAndEmailAreUniqueForUpdate(informationsToUpdate);
        if (informationsToUpdate.getPassword() != null) {
            informationsToUpdate.setPassword(passwordEncoder.encode(informationsToUpdate.getPassword()));
        }
        updateUserInformation(currentUser, informationsToUpdate);
    }

    private void ensureUsernameAndEmailAreUniqueForUpdate(final InformationsToUpdate informationsToUpdate) {
        if (informationsToUpdate.getUsername() != null || informationsToUpdate.getEmail() != null) {
            ensureUsernameAndEmailAreUnique(informationsToUpdate.getUsername(), informationsToUpdate.getEmail());
        }
    }

    private void updateUserInformation(final AppUser currentUser, final InformationsToUpdate informationsToUpdate) {
        appUserRepository.save(AppUserMapper.INSTANCE.updateUserInformation(currentUser, informationsToUpdate));
    }

    private void validateInformationsToUpdate(final InformationsToUpdate informationsToUpdate) {
        if (verifyInformationsToUpdate(informationsToUpdate)) {
            throw new IllegalArgumentException("the form must have some values");
        }
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

    private boolean verifyInformationsToUpdate(final InformationsToUpdate informationsToUpdate) {
        return Stream.of(
                        informationsToUpdate.getEmail(),
                        informationsToUpdate.getUsername(),
                        informationsToUpdate.getPassword()
                )
                .allMatch(Strings::isBlank);
    }
}
