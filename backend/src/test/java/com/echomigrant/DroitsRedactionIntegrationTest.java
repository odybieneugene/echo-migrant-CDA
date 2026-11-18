package com.echomigrant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Droits de la rédaction sur les articles :
 * un rédacteur modifie seulement ses propres articles et ne supprime rien ;
 * journaliste et admin modifient tous les articles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DroitsRedactionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void redacteur_modifieSonArticle_maisPasCeluiDUnCollegue() throws Exception {
        String admin = connecter("admin@echomigrant.local", "ChangeMoi123!");
        String redacteurA = compteAvecRole(admin, "redacteur");
        String redacteurB = compteAvecRole(admin, "redacteur");

        Long idArticleA = creerArticle(redacteurA, "Article du rédacteur A");

        modifier(redacteurA, idArticleA, "Modifié par son auteur").andExpect(status().isOk())
            .andExpect(jsonPath("$.titre").value("Modifié par son auteur"));

        modifier(redacteurB, idArticleA, "Modifié par un collègue")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Un rédacteur ne peut modifier que ses propres articles"));
    }

    @Test
    void journalisteEtAdmin_modifientLesArticlesDesAutres() throws Exception {
        String admin = connecter("admin@echomigrant.local", "ChangeMoi123!");
        String redacteur = compteAvecRole(admin, "redacteur");
        String journaliste = compteAvecRole(admin, "journaliste");
        Long id = creerArticle(redacteur, "Article à relire");

        modifier(journaliste, id, "Relu par le journaliste").andExpect(status().isOk());
        modifier(admin, id, "Relu par l'admin").andExpect(status().isOk());
    }

    @Test
    void redacteur_neSupprimePas_journalisteOui() throws Exception {
        String admin = connecter("admin@echomigrant.local", "ChangeMoi123!");
        String redacteur = compteAvecRole(admin, "redacteur");
        String journaliste = compteAvecRole(admin, "journaliste");
        Long id = creerArticle(redacteur, "Article à supprimer");

        mockMvc.perform(delete("/api/articles/" + id).header("Authorization", "Bearer " + redacteur))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/articles/" + id).header("Authorization", "Bearer " + journaliste))
            .andExpect(status().isNoContent());
    }

    // ---- utilitaires ----

    /** Inscrit un compte (lecteur), lui donne le rôle demandé via l'admin, puis renvoie son jeton. */
    private String compteAvecRole(String jetonAdmin, String role) throws Exception {
        String email = "redac." + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        String body = mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "nom", "Test", "prenom", role, "email", email, "password", "motdepasse123"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("user").get("id").asLong();

        mockMvc.perform(put("/api/utilisateurs/" + id + "/roles")
                .header("Authorization", "Bearer " + jetonAdmin)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("roles", List.of(role)))))
            .andExpect(status().isOk());
        // Nouveau jeton après le changement de rôle (les rôles sont relus en base à chaque requête)
        return connecter(email, "motdepasse123");
    }

    private Long creerArticle(String jeton, String titre) throws Exception {
        String body = mockMvc.perform(multipart("/api/articles")
                .file(partieData(titre))
                .header("Authorization", "Bearer " + jeton))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions modifier(String jeton, Long id, String titre) throws Exception {
        return mockMvc.perform(multipart(HttpMethod.PUT, "/api/articles/" + id)
            .file(partieData(titre))
            .header("Authorization", "Bearer " + jeton));
    }

    private MockMultipartFile partieData(String titre) throws Exception {
        return new MockMultipartFile("data", "", "application/json",
            objectMapper.writeValueAsBytes(Map.of("titre", titre, "resume", "Résumé", "contenu", "Contenu")));
    }

    private String connecter(String email, String motDePasse) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", motDePasse))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
