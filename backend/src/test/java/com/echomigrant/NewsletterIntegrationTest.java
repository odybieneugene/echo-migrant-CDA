package com.echomigrant;

import com.echomigrant.repository.InscriptionNewsletterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** POST /api/newsletter public et idempotent ; consultation et suppression réservées à l'admin. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NewsletterIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private InscriptionNewsletterRepository repository;

    @Test
    void inscriptionAnonyme_enregistreeEnBase() throws Exception {
        String email = emailUnique();

        inscrire(email)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Inscription à la newsletter enregistrée"));

        assertThat(repository.existsByEmail(email)).isTrue();
    }

    @Test
    void memeEmailDeuxFois_memeReponse_etUneSeuleLigne() throws Exception {
        String email = emailUnique();

        inscrire(email).andExpect(status().isOk());
        // Majuscules et espaces : c'est le même email une fois normalisé
        inscrire("  " + email.toUpperCase() + " ").andExpect(status().isOk());

        long lignes = repository.findAll().stream().filter(i -> i.getEmail().equals(email)).count();
        assertThat(lignes).isEqualTo(1);
    }

    @Test
    void emailInvalide_400() throws Exception {
        inscrire("pas-un-email")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.email").value("Email invalide"));
        inscrire("").andExpect(status().isBadRequest());
    }

    @Test
    void listeDesInscrits_adminUniquement() throws Exception {
        String email = emailUnique();
        inscrire(email);

        mockMvc.perform(get("/api/newsletter")).andExpect(status().isUnauthorized());

        String lecteur = inscrireCompte();
        mockMvc.perform(get("/api/newsletter").header("Authorization", "Bearer " + lecteur))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/newsletter").header("Authorization", "Bearer " + connecterAdmin()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.email == '" + email + "')]").exists());
    }

    @Test
    void suppressionParAdmin_204_puis404() throws Exception {
        String email = emailUnique();
        inscrire(email);
        Long id = repository.findAll().stream().filter(i -> i.getEmail().equals(email)).findFirst().orElseThrow().getId();
        String admin = connecterAdmin();

        mockMvc.perform(delete("/api/newsletter/" + id).header("Authorization", "Bearer " + admin))
            .andExpect(status().isNoContent());
        assertThat(repository.existsByEmail(email)).isFalse();

        mockMvc.perform(delete("/api/newsletter/" + id).header("Authorization", "Bearer " + admin))
            .andExpect(status().isNotFound());
    }

    // ---- utilitaires ----

    private String emailUnique() {
        return "news." + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
    }

    private ResultActions inscrire(String email) throws Exception {
        return mockMvc.perform(post("/api/newsletter")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("email", email))));
    }

    private String inscrireCompte() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "nom", "Test", "prenom", "News", "email", emailUnique(), "password", "motdepasse123"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
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
