package com.echomigrant;

import com.echomigrant.entity.Article;
import com.echomigrant.entity.Commentaire;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.repository.ArticleRepository;
import com.echomigrant.repository.CommentaireRepository;
import com.echomigrant.repository.StatutRepository;
import com.echomigrant.repository.UtilisateurRepository;
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
 * Lecture publique des commentaires d'un article visible, mais pas de ceux d'un brouillon ;
 * liste complète réservée à l'admin ; impossible de commenter un brouillon.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentaireIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private StatutRepository statutRepository;
    @Autowired private CommentaireRepository commentaireRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;

    @Test
    void visiteur_litLesCommentairesDUnArticlePublie() throws Exception {
        Long articleId = creerArticleAvecCommentaire("publie", "Bravo pour cet article");

        mockMvc.perform(get("/api/commentaires").param("articleId", articleId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].contenu").value("Bravo pour cet article"));
    }

    @Test
    void visiteur_neLitPasLesCommentairesDUnBrouillon() throws Exception {
        Long articleId = creerArticleAvecCommentaire("brouillon", "Commentaire interne");

        mockMvc.perform(get("/api/commentaires").param("articleId", articleId.toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    void listeComplete_refuseeAuVisiteur_401() throws Exception {
        mockMvc.perform(get("/api/commentaires"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listeComplete_refuseeAuLecteur_403() throws Exception {
        String token = inscrireLecteur("lecteur.commentaires@example.com");

        mockMvc.perform(get("/api/commentaires").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void listeComplete_autoriseePourLAdmin() throws Exception {
        String token = connecter("admin@echomigrant.local", "ChangeMoi123!");

        mockMvc.perform(get("/api/commentaires").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void lecteur_nePeutPasCommenterUnBrouillon() throws Exception {
        Long articleId = creerArticleAvecCommentaire("brouillon", "Commentaire interne bis");
        String token = inscrireLecteur("lecteur.brouillon@example.com");

        mockMvc.perform(post("/api/commentaires")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("articleId", articleId, "contenu", "Je n'aurais pas dû voir ça"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void lecteur_peutCommenterUnArticlePublie() throws Exception {
        Long articleId = creerArticleAvecCommentaire("publie", "Premier commentaire");
        String token = inscrireLecteur("lecteur.publie@example.com");

        mockMvc.perform(post("/api/commentaires")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of("articleId", articleId, "contenu", "Très instructif"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.contenu").value("Très instructif"));
    }

    private Long creerArticleAvecCommentaire(String statut, String contenu) {
        Article article = new Article();
        article.setTitre("Article " + statut);
        article.setStatut(statutRepository.findByNomIgnoreCase(statut).orElseThrow());
        article = articleRepository.save(article);

        Utilisateur admin = utilisateurRepository.findByEmail("admin@echomigrant.local").orElseThrow();
        Commentaire commentaire = new Commentaire();
        commentaire.setContenu(contenu);
        commentaire.setArticle(article);
        commentaire.setUtilisateur(admin);
        commentaireRepository.save(commentaire);

        return article.getId();
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
