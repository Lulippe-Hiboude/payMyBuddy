package com.lulippe.paymybuddy.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulippe.paymybuddy.persistence.entities.AppUser;
import com.lulippe.paymybuddy.persistence.repository.AppUserRepository;
import com.lulippe.paymybuddy.user.model.InformationsToUpdate;
import com.lulippe.paymybuddy.user.model.RegisterRequest;
import com.lulippe.paymybuddy.user.model.UserFriend;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("should add a new friend")
    public void addNewFriend() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user1 = createUserInDB(username, email, password);

        final String username2 = "Christian Clavier";
        final String email2 = "cc@mail.com";
        final String password2 = "123";
        final AppUser user2 = createUserInDB(username2, email2, password2);

        final String expectedJson = "User added";
        //when
        mockMvc.perform(post("/users/me/friends/v0")
                        .with(csrf())
                        .with(user(user1.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("friendEmail", user2.getEmail()))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));

        //then
        final AppUser refreshUser1 = appUserRepository.findByUsername(user1.getUsername())
                .orElseThrow(() -> new AssertionError("Expected user not found"));
        assertNotNull(refreshUser1.getFriends());
        assertTrue(refreshUser1.getFriends().stream()
                .anyMatch(friend -> friend.getEmail().equals(user2.getEmail())));
    }

    @Test
    @DisplayName("should not be able to add yourself as friend")
    void ShouldNotBeAbleToAddYourselfAsFriend() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user1 = createUserInDB(username, email, password);

        final String expectedJson = "You cannot add yourself as a friend! that is sad :(";
        //when
        mockMvc.perform(post("/users/me/friends/v0")
                        .with(csrf())
                        .with(user(user1.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("friendEmail", user1.getEmail()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("should return EntityAlreadyExistsException")
    void ShouldReturnEntityAlreadyExistsException() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user1 = createUserInDB(username, email, password);

        final String username2 = "Christian Clavier";
        final String email2 = "cc@mail.com";
        final String password2 = "123";
        final AppUser user2 = createUserInDB(username2, email2, password2);

        final AppUser refreshUser1 = addFriendToUserInDB(user1, user2);
        final String expectedJson = "Friend already added in current user friend list : " + user2.getEmail();

        //when & then
        mockMvc.perform(post("/users/me/friends/v0")
                        .with(csrf())
                        .with(user(refreshUser1.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("friendEmail", user2.getEmail()))
                .andExpect(status().isConflict())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("should return user friendList")
    void ShouldReturnUserFriendList() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user1 = createUserInDB(username, email, password);

        final String username2 = "Christian Clavier";
        final String email2 = "cc@mail.com";
        final String password2 = "123";
        final AppUser user2 = createUserInDB(username2, email2, password2);

        final AppUser refreshUser1 = addFriendToUserInDB(user1, user2);
        final UserFriend userFriend = new UserFriend();
        userFriend.setFriendName(user2.getUsername());
        final List<UserFriend> userFriends = Collections.singletonList(userFriend);
        final String expectedJson = objectMapper.writeValueAsString(userFriends);

        //when & then
        mockMvc.perform(get("/users/me/friends/v0")
                        .with(csrf())
                        .with(user(refreshUser1.getEmail()).roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("should return and empty friend list ")
    void ShouldReturnAndEmptyFriendList() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final AppUser user1 = createUserInDB(username, email, password);

        final List<UserFriend> userFriends = Collections.emptyList();
        final String expectedJson = objectMapper.writeValueAsString(userFriends);

        //when & then
        mockMvc.perform(get("/users/me/friends/v0")
                        .with(csrf())
                        .with(user(user1.getEmail()).roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));
    }

    @Test
    @DisplayName("should update profil of user")
    void ShouldUpdateProfileOfUser() throws Exception {
        //given
        final String username = "Jean Reno";
        final String email = "jean.reno@mail.com";
        final String password = "123";
        final String newPassword = "123456";
        final String newUsername = "JeanReno";
        final String newEmail = null;

        final AppUser user1 = createUserInDB(username, email, password);
        final InformationsToUpdate informationsToUpdate = new InformationsToUpdate();
        informationsToUpdate.setUsername(newUsername);
        informationsToUpdate.setEmail(newEmail);
        informationsToUpdate.setPassword(newPassword);

        final String expectedJson = objectMapper.writeValueAsString(informationsToUpdate);

        //when & then
        mockMvc.perform(patch("/users/me/v0")
                .with(csrf())
                .with(user(user1.getEmail()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(informationsToUpdate))
        ).andExpect(status().isOk())
        .andExpect(content().string("User information updated successfully"));

        final AppUser appUser = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new AssertionError("Expected user not found"));

        assertEquals(email, appUser.getEmail());
        assertEquals(newUsername, appUser.getUsername());
    }

    private AppUser createUserInDB(final String username, final String email, final String password) throws Exception {
        final String expectedJson = "user registered successfully";
        final RegisterRequest.RoleEnum role = RegisterRequest.RoleEnum.USER;
        final RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        registerRequest.setRole(role);
        mockMvc.perform(post("/auth/register/v0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string(expectedJson));

        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new AssertionError("User not found in DB"));
    }

    private AppUser addFriendToUserInDB(final AppUser user1, final AppUser user2) throws Exception {
        final String expectedJson = "User added";
        mockMvc.perform(post("/users/me/friends/v0")
                        .with(csrf())
                        .with(user(user1.getEmail()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("friendEmail", user2.getEmail()))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedJson));

        return appUserRepository.findByUsername(user1.getUsername())
                .orElseThrow(() -> new AssertionError("Expected user not found"));
    }
}
