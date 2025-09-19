package com.lulippe.paymybuddy.web;

import com.lulippe.paymybuddy.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = HomeController.class)
@Import(TestSecurityConfig.class)
class HomeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("should show transfer form")
    void shouldShowTransferForm() throws Exception {
        mockMvc.perform(get("/home/transfer"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/transfer"));
    }

    @Test
    @DisplayName("should show add friend form")
    void shouldShowAddFriendForm() throws Exception {
        mockMvc.perform(get("/home/friends/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/friendsAdd"));
    }

    @Test
    @DisplayName("should show profil form")
    void shouldShowProfilForm() throws Exception {
        mockMvc.perform(get("/home/profil"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/profil"));
    }
}