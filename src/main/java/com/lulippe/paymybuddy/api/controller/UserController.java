package com.lulippe.paymybuddy.api.controller;

import com.lulippe.paymybuddy.service.UserService;
import com.lulippe.paymybuddy.user.api.UsersConnectionsApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import static com.lulippe.paymybuddy.utils.AuthenticationUtil.getAuthenticatedUserEmail;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController implements UsersConnectionsApi {
    private final UserService userService;

    @Override
    public ResponseEntity<String> addNewFriend(final String friendEmail) {
        final String userEmail = getAuthenticatedUserEmail();
        userService.handleFriendAddition(userEmail, friendEmail);
        return ResponseEntity.ok("User added");
    }
}
