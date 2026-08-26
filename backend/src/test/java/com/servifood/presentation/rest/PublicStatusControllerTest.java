package com.servifood.presentation.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PublicStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesPublicApplicationStatus() throws Exception {
        mockMvc.perform(get("/api/v1/public/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("ServiFood"))
                .andExpect(jsonPath("$.status").value("available"));
    }

    @Test
    void deniesUnconfiguredRoutes() throws Exception {
        mockMvc.perform(get("/api/private"))
                .andExpect(status().isUnauthorized());
    }
}
