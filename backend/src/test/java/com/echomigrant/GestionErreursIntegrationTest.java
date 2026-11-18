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
 * Une erreur du client doit donner un 4xx explicite au format ApiError,
 * et non un 500 avec le message technique interne.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GestionErreursIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void jsonMalForme_400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("{\"nom\": \"Test\", "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Corps de requête invalide (JSON mal formé ou mal encodé)"));
    }

    @Test
    void jsonMalEncode_400() throws Exception {
        // "é" en Latin-1 (0xE9) dans un corps déclaré UTF-8 : cas réel rencontré avec un terminal Windows
        byte[] corps = "{\"nom\":\"Démo\"}".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(corps))
            .andExpect(status().isBadRequest());
    }

    @Test
    void identifiantNonNumerique_400() throws Exception {
        mockMvc.perform(get("/api/articles/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Paramètre invalide : id"));
    }

    @Test
    void routeInexistante_404() throws Exception {
        String token = connecterAdmin();
        mockMvc.perform(get("/api/route-qui-n-existe-pas").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Ressource introuvable"));
    }

    @Test
    void methodeNonAutorisee_405() throws Exception {
        mockMvc.perform(delete("/api/auth/login"))
            .andExpect(status().isMethodNotAllowed());
    }

    private String connecterAdmin() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", "admin@echomigrant.local", "password", "ChangeMoi123!"))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
