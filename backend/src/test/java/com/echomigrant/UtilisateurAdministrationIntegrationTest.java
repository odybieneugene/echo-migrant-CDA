package com.echomigrant;

import com.echomigrant.repository.UtilisateurRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Administration des comptes : l'admin attribue les rôles (l'inscription donne toujours "lecteur"),
 * avec des garde-fous pour ne jamais se retrouver sans administrateur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UtilisateurAdministrationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UtilisateurRepository utilisateurRepository;

    @Test
    void admin_promeutUnLecteurJournaliste_quiAccedeAussitotAuDashboard() throws Exception {
        String admin = connecter("admin@echomigrant.local", "ChangeMoi123!");
        JsonNode lecteur = inscrire("futur.journaliste@example.com");
        long id = lecteur.get("user").get("id").asLong();
        String tokenLecteur = lecteur.get("token").asText();

        mockMvc.perform(get("/api/articles/admin").header("Authorization", "Bearer " + tokenLecteur))
            .andExpect(status().isForbidden());

        changerRoles(admin, id, List.of("journaliste"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles.length()").value(1))
            .andExpect(jsonPath("$.roles[0]").value("journaliste"));

        // Les rôles sont relus en base à chaque requête : le même token suffit
        mockMvc.perform(get("/api/articles/admin").header("Authorization", "Bearer " + tokenLecteur))
            .andExpect(status().isOk());
    }

    @Test
    void lecteur_nePeutPasChangerDeRoles_403() throws Exception {
        JsonNode lecteur = inscrire("lecteur.ambitieux@example.com");
        long id = lecteur.get("user").get("id").asLong();

        changerRoles(lecteur.get("token").asText(), id, List.of("admin"))
            .andExpect(status().isForbidden());
    }

    @Test
    void roleInconnu_400() throws Exception {
        String admin = connecter("admin@echomigrant.local", "ChangeMoi123!");
        long id = inscrire("role.inconnu@example.com").get("user").get("id").asLong();

        changerRoles(admin, id, List.of("superadmin"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Rôle inconnu : superadmin"));
    }

    @Test
    void listeDeRolesVide_400() throws Exception {
        String admin = connecter("admin@echomigrant.local", "ChangeMoi123!");
        long id = inscrire("roles.vides@example.com").get("user").get("id").asLong();

        changerRoles(admin, id, List.of())
            .andExpect(status().isBadRequest());
    }

    @Test
    void admin_nePeutPasRetirerSonPropreRoleAdmin() throws Exception {
        String admin = connecter("admin@echomigrant.local", "ChangeMoi123!");
        long adminId = utilisateurRepository.findByEmail("admin@echomigrant.local").orElseThrow().getId();

        changerRoles(admin, adminId, List.of("lecteur"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Vous ne pouvez pas retirer votre propre rôle administrateur"));
    }

    @Test
    void admin_nePeutPasSupprimerSonPropreCompte() throws Exception {
        String admin = connecter("admin@echomigrant.local", "ChangeMoi123!");
        long adminId = utilisateurRepository.findByEmail("admin@echomigrant.local").orElseThrow().getId();

        mockMvc.perform(delete("/api/utilisateurs/" + adminId).header("Authorization", "Bearer " + admin))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Vous ne pouvez pas supprimer votre propre compte"));
    }

    @Test
    void admin_supprimeUnCompte() throws Exception {
        String admin = connecter("admin@echomigrant.local", "ChangeMoi123!");
        long id = inscrire("a.supprimer@example.com").get("user").get("id").asLong();

        mockMvc.perform(delete("/api/utilisateurs/" + id).header("Authorization", "Bearer " + admin))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/utilisateurs/" + id).header("Authorization", "Bearer " + admin))
            .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions changerRoles(String token, long id, List<String> roles) throws Exception {
        return mockMvc.perform(put("/api/utilisateurs/" + id + "/roles")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of("roles", roles))));
    }

    private JsonNode inscrire(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "nom", "Test", "prenom", "Utilisateur", "email", email, "password", "motdepasse123"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String connecter(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
