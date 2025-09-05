package com.lulippe.paymybuddy.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.service.UserService;
import com.lulippe.paymybuddy.user.model.UserFriend;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should add new friend")
    void shouldAddNewFriend() throws Exception {
        //given
        final String friendEmail = "friend@test.com";
        final String userEmail = "test@test.com";
        doNothing().when(userService).handleFriendAddition(userEmail, friendEmail);

        //when & then
        mockMvc.perform(post("/users/me/friends/v0")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("friendEmail", friendEmail)
                )
                .andExpect(status().isOk())
                .andExpect(content().string("User added"));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return CONFLICT")
    void shouldReturnConflict() throws Exception {
        //given
        final String friendEmail = "friend@test.com";
        final String userEmail = "test@test.com";
        doThrow(new EntityAlreadyExistsException("Friend already added in current user friend list : " + friendEmail)).when(userService).handleFriendAddition(userEmail, friendEmail);

        //when & then
        mockMvc.perform(post("/users/me/friends/v0")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("friendEmail", friendEmail)
                )
                .andExpect(status().isConflict())
                .andExpect(content().string("Friend already added in current user friend list : " + friendEmail));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return BAD REQUEST")
    void shouldReturnBadRequest() throws Exception {
        //given
        final String friendEmail = "friend@test.com";
        final String userEmail = "test@test.com";
        doThrow(new IllegalArgumentException("You cannot add yourself as a friend! that is sad :(")).when(userService).handleFriendAddition(userEmail, friendEmail);


        //when & then
        mockMvc.perform(post("/users/me/friends/v0")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("friendEmail", friendEmail)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You cannot add yourself as a friend! that is sad :("));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return a list of UserFriend")
    void shouldReturnListOfUserFriend() throws Exception {
        final String userEmail = "test@test.com";
        final String friendName1 = "Albert";
        final String friendName2 = "Bernard";
        final UserFriend userFriend1 = new UserFriend();
        userFriend1.setFriendName(friendName1);
        final UserFriend userFriend2 = new UserFriend();
        userFriend2.setFriendName(friendName2);

        final List<UserFriend> userFriendList = List.of(userFriend1, userFriend2);
        given(userService.getAllUserFriend(userEmail)).willReturn(userFriendList);

        //when & then
        mockMvc.perform(get("/users/me/friends/v0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].friendName").value(friendName1))
                .andExpect(jsonPath("$[1].friendName").value(friendName2));
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = "USER")
    @DisplayName("should return an empty list of UserFriend")
    void shouldReturnAnEmptyListOfUserFriend() throws Exception {
        final String userEmail = "test@test.com";

        final List<UserFriend> userFriendList = Collections.emptyList();
        given(userService.getAllUserFriend(userEmail)).willReturn(userFriendList);

        //when & then
        mockMvc.perform(get("/users/me/friends/v0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

}