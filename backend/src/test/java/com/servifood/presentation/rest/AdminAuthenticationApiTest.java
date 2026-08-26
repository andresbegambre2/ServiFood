package com.servifood.presentation.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.servifood.domain.model.InternalUser;
import com.servifood.domain.model.UserRole;
import com.servifood.infrastructure.persistence.InternalUserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminAuthenticationApiTest {
    @Autowired MockMvc mvc;
    @Autowired InternalUserRepository users;
    @Autowired PasswordEncoder passwords;

    @BeforeEach
    void setUp() {
        users.save(new InternalUser("Admin Test", "admin-test@servifood.local", passwords.encode("correct-password"), UserRole.ADMIN));
    }

    @Test
    void logsInReadsSessionAndLogsOutSafely() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/v1/admin/auth/login").with(csrf())
                        .param("username", "admin-test@servifood.local").param("password", "correct-password"))
                .andExpect(status().isNoContent()).andExpect(authenticated().withRoles("ADMIN"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/v1/admin/auth/session").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin Test")).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(post("/api/v1/admin/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/auth/session").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidCredentialsAndAnonymousAdminAccess() throws Exception {
        mvc.perform(post("/api/v1/admin/auth/login").with(csrf()).param("username", "admin-test@servifood.local").param("password", "wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/orders")).andExpect(status().isUnauthorized());
    }
}
