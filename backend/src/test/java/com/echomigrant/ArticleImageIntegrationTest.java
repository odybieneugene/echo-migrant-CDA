package com.echomigrant;

import com.echomigrant.entity.Article;
import com.echomigrant.repository.ArticleRepository;
import com.echomigrant.repository.StatutRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Image de couverture : fichier envoyé (servi par /uploads/) ou URL externe (laissée telle quelle).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArticleImageIntegrationTest {

    private static final String UNSPLASH = "https://images.unsplash.com/photo-1509099836639-18ba1795216d?q=80&w=1200";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private StatutRepository statutRepository;

    @Test
    void imageExterne_estRenvoyeeTelleQuelle() throws Exception {
        Long id = creerArticlePublie("Article image externe", UNSPLASH);

        mockMvc.perform(get("/api/articles/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imageCouverture").value(UNSPLASH));
    }

    @Test
    void imageLocale_estServieParUploads() throws Exception {
        Long id = creerArticlePublie("Article image locale", "images/exemple.jpg");

        mockMvc.perform(get("/api/articles/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imageCouverture").value(startsWith("http")))
            .andExpect(jsonPath("$.imageCouverture").value(endsWith("/uploads/images/exemple.jpg")));
    }

    @Test
    void remplacerUneImageExterne_parUnFichier_nePlantePas() throws Exception {
        // Avant : deleteImage("https://...") -> chemin invalide sous Windows -> erreur 500
        Long id = creerArticlePublie("Article dont on change l'image", UNSPLASH);
        String token = connecterAdmin();

        MockMultipartFile data = new MockMultipartFile("data", "", "application/json",
            objectMapper.writeValueAsBytes(Map.of("titre", "Article dont on change l'image")));
        MockMultipartFile image = new MockMultipartFile("image", "nouvelle.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/articles/" + id)
                .file(data).file(image)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imageCouverture").value(endsWith(".png")));
    }

    private Long creerArticlePublie(String titre, String image) {
        Article article = new Article();
        article.setTitre(titre);
        article.setImageCouverture(image);
        article.setStatut(statutRepository.findByNomIgnoreCase("publie").orElseThrow());
        return articleRepository.save(article).getId();
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
