package com.lulippe.paymybuddy.service;

import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.api.exception.NonexistentEntityException;
import com.lulippe.paymybuddy.mapper.AppUserMapper;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.repository.AppUserRepository;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

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
        appUserRepository.save(sender);
        appUserRepository.save(receiver);
    }
}
