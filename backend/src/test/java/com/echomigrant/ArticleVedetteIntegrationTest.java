package com.echomigrant;

import com.echomigrant.repository.ArticleRepository;
import com.echomigrant.repository.UtilisateurRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Un seul article "en vedette" à la fois, et requêtes ciblées (plus de findAll()).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArticleVedetteIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;

    @Test
    void creerUnArticleEnVedette_retireLaVedetteALAncien() throws Exception {
        String token = connecterAdmin();
        long ancien = creerArticle(token, "Ancienne une", true);
        long nouveau = creerArticle(token, "Nouvelle une", true);

        assertThat(estEnVedette(ancien)).isFalse();
        assertThat(estEnVedette(nouveau)).isTrue();
        assertThat(articleRepository.findByEstEnVedetteTrue()).hasSize(1);
    }

    @Test
    void mettreAJourUnArticleEnVedette_leGardeEnVedetteEtRetireLesAutres() throws Exception {
        String token = connecterAdmin();
        long autre = creerArticle(token, "Une actuelle", true);
        long article = creerArticle(token, "Futur article à la une", false);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/articles/" + article)
                .file(partieData("Futur article à la une", true))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estEnVedette").value(true));

        assertThat(estEnVedette(article)).isTrue();
        assertThat(estEnVedette(autre)).isFalse();
        assertThat(articleRepository.findByEstEnVedetteTrue()).hasSize(1);
    }

    @Test
    void creerUnArticleSansVedette_neTouchePasALaUneActuelle() throws Exception {
        String token = connecterAdmin();
        long une = creerArticle(token, "La une reste", true);
        creerArticle(token, "Article ordinaire", false);

        assertThat(estEnVedette(une)).isTrue();
    }

    @Test
    void detectionDeLAdmin_parRequeteExists() {
        // Utilisé par DataInitializer au démarrage (remplace un findAll() de tous les utilisateurs)
        // (base H2 partagée entre classes de test : on ne suppose rien sur les autres rôles)
        assertThat(utilisateurRepository.existsByRoles_NomIgnoreCase("admin")).isTrue();
        assertThat(utilisateurRepository.existsByRoles_NomIgnoreCase("role-inexistant")).isFalse();
    }

    private long creerArticle(String token, String titre, boolean enVedette) throws Exception {
        String body = mockMvc.perform(multipart("/api/articles")
                .file(partieData(titre, enVedette))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private MockMultipartFile partieData(String titre, boolean enVedette) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("titre", titre);
        data.put("estEnVedette", enVedette);
        return new MockMultipartFile("data", "", "application/json", objectMapper.writeValueAsBytes(data));
    }

    private boolean estEnVedette(long id) {
        return articleRepository.findById(id).orElseThrow().isEstEnVedette();
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
