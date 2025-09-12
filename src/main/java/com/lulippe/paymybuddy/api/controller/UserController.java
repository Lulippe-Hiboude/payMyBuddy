package com.lulippe.paymybuddy.api.controller;

import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.service.UserService;
import com.lulippe.paymybuddy.user.api.UserApi;
import com.lulippe.paymybuddy.user.api.UsersConnectionsApi;
import com.lulippe.paymybuddy.user.model.InformationsToUpdate;
import com.lulippe.paymybuddy.user.model.UserFriend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.lulippe.paymybuddy.utils.AuthenticationUtil.getAuthenticatedUserEmail;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController implements UsersConnectionsApi, UserApi {
    private final UserService userService;

    @Override
    public ResponseEntity<String> addNewFriend(final String friendEmail) {
        final String userEmail = getAuthenticatedUserEmail();
        userService.handleFriendAddition(userEmail, friendEmail);
        return ResponseEntity.ok("User added");
    }

    @Override
    public ResponseEntity<List<UserFriend>> getUserFriendList() {
        final String userEmail = getAuthenticatedUserEmail();
        log.info("Getting user friend list");
        final List<UserFriend> userFriendList = userService.getAllUserFriend(userEmail);
        return ResponseEntity.ok(userFriendList);
    }

    @Override
    public ResponseEntity<String> updateUserProfile(final InformationsToUpdate informationsToUpdate) {
        final String userEmail = getAuthenticatedUserEmail();
        log.info("Start updating user profile");
        userService.updateUserProfil(userEmail,informationsToUpdate);
        return ResponseEntity.ok("User information updated successfully");
    }
}
