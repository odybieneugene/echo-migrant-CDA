package com.echomigrant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de bout en bout du parcours d'authentification, équivalent du flux
 * Register -> Login -> Profile validé côté Laravel avec Sanctum.
 * Tourne sur une base H2 en mémoire (voir application-test.yml), sans dépendance à PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EchoMigrantAuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenLoginThenAccessProtectedProfile() throws Exception {
        Map<String, String> registerPayload = Map.of(
            "nom", "Bien Eugene",
            "prenom", "Ody",
            "email", "ody.test@example.com",
            "password", "motdepasse123"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(registerPayload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value("ody.test@example.com"));

        Map<String, String> loginPayload = Map.of(
            "email", "ody.test@example.com",
            "password", "motdepasse123"
        );

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ody.test@example.com"));
    }

    @Test
    void registerIgnoresRoleSentByClient_userIsAlwaysLecteur() throws Exception {
        Map<String, String> payload = Map.of(
            "nom", "Pirate",
            "prenom", "Test",
            "email", "pirate@example.com",
            "password", "motdepasse123",
            "role", "admin"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.roles.length()").value(1))
            .andExpect(jsonPath("$.user.roles[0]").value("lecteur"));
    }

    @Test
    void publicArticlesEndpointIsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/articles"))
            .andExpect(status().isOk());
    }

    @Test
    void adminArticlesEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/articles/admin"))
            .andExpect(status().isUnauthorized());
    }
}
