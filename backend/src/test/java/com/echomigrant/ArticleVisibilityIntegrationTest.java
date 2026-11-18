package com.echomigrant;

import com.echomigrant.entity.Article;
import com.echomigrant.repository.ArticleRepository;
import com.echomigrant.repository.StatutRepository;
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
 * Un brouillon ne doit pas être lisible publiquement via GET /api/articles/{id},
 * mais reste consultable par l'équipe éditoriale (prévisualisation).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArticleVisibilityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private StatutRepository statutRepository;

    @Test
    void brouillon_estInvisiblePourUnVisiteur() throws Exception {
        Long id = creerArticle("Brouillon secret", "brouillon");

        mockMvc.perform(get("/api/articles/" + id))
            .andExpect(status().isNotFound());
    }

    @Test
    void brouillon_estInvisiblePourUnLecteurConnecte() throws Exception {
        Long id = creerArticle("Brouillon secret lecteur", "brouillon");
        String token = inscrireLecteur("lecteur.visibilite@example.com");

        mockMvc.perform(get("/api/articles/" + id).header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void brouillon_estVisiblePourLEquipeEditoriale() throws Exception {
        Long id = creerArticle("Brouillon en relecture", "brouillon");
        String token = connecter("admin@echomigrant.local", "ChangeMoi123!");

        mockMvc.perform(get("/api/articles/" + id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titre").value("Brouillon en relecture"));
    }

    @Test
    void articlePublie_estVisiblePourUnVisiteur() throws Exception {
        Long id = creerArticle("Article publié", "publie");

        mockMvc.perform(get("/api/articles/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titre").value("Article publié"));
    }

    @Test
    void articleSansStatut_estInvisiblePourUnVisiteur() throws Exception {
        Long id = creerArticle("Article sans statut", null);

        mockMvc.perform(get("/api/articles/" + id))
            .andExpect(status().isNotFound());
    }

    private Long creerArticle(String titre, String statut) {
        Article article = new Article();
        article.setTitre(titre);
        if (statut != null) {
            article.setStatut(statutRepository.findByNomIgnoreCase(statut).orElseThrow());
        }
        return articleRepository.save(article).getId();
    }

    private String inscrireLecteur(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "nom", "Lecteur", "prenom", "Test", "email", email, "password", "motdepasse123"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
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
