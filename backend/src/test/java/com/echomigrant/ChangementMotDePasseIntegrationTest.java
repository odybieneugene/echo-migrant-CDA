package com.echomigrant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** PUT /api/auth/password : réservé à l'utilisateur connecté, qui doit fournir son mot de passe actuel. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangementMotDePasseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void changementReussi_ancienRefuse_nouveauAccepte() throws Exception {
        String email = emailUnique();
        String token = inscrire(email, "ancien123");

        changer(token, "ancien123", "nouveau456").andExpect(status().isNoContent());

        connexion(email, "ancien123").andExpect(status().isUnauthorized());
        connexion(email, "nouveau456").andExpect(status().isOk());
    }

    @Test
    void motDePasseActuelFaux_400_etRienNeChange() throws Exception {
        String email = emailUnique();
        String token = inscrire(email, "ancien123");

        changer(token, "mauvais", "nouveau456")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Mot de passe actuel incorrect"));

        connexion(email, "ancien123").andExpect(status().isOk());
    }

    @Test
    void nouveauIdentiqueOuTropCourt_400() throws Exception {
        String token = inscrire(emailUnique(), "ancien123");

        changer(token, "ancien123", "ancien123")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Le nouveau mot de passe doit être différent de l'actuel"));
        changer(token, "ancien123", "abc")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.nouveauMotDePasse").exists());
    }

    @Test
    void anonyme_401() throws Exception {
        mockMvc.perform(put("/api/auth/password")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "motDePasseActuel", "x", "nouveauMotDePasse", "nouveau456"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void profilAnonyme_401() throws Exception {
        // /api/auth/** est public : avant correction, /me sans token plantait en 500 (utilisateur null)
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    // ---- utilitaires ----

    private String emailUnique() {
        return "mdp." + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
    }

    private String inscrire(String email, String motDePasse) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "nom", "Test", "prenom", "Mdp", "email", email, "password", motDePasse))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private org.springframework.test.web.servlet.ResultActions connexion(String email, String motDePasse) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("email", email, "password", motDePasse))));
    }

    private org.springframework.test.web.servlet.ResultActions changer(String token, String actuel, String nouveau) throws Exception {
        return mockMvc.perform(put("/api/auth/password")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of(
                "motDePasseActuel", actuel, "nouveauMotDePasse", nouveau))));
    }
}
