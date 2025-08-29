package com.lulippe.paymybuddy.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lulippe.paymybuddy.api.exception.EntityAlreadyExistsException;
import com.lulippe.paymybuddy.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        mockMvc.perform(post("/users/connections/v0")
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
        mockMvc.perform(post("/users/connections/v0")
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
        mockMvc.perform(post("/users/connections/v0")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("friendEmail", friendEmail)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string("You cannot add yourself as a friend! that is sad :("));
    }

}