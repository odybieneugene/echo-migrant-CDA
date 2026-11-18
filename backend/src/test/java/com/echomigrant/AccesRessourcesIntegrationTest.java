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

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Accès aux ressources d'un autre utilisateur : 403 avec un corps ApiError explicite
 * (et non 400, ni un 403 sans corps).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccesRessourcesIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private StatutRepository statutRepository;
    @Autowired private CommentaireRepository commentaireRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;

    @Test
    void abonnementDUnAutre_lecture_403() throws Exception {
        String tokenProprietaire = inscrire("proprio.abo.lecture@example.com");
        String tokenAutre = inscrire("autre.abo.lecture@example.com");
        long abonnementId = creerAbonnement(tokenProprietaire);

        mockMvc.perform(get("/api/abonnements/" + abonnementId).header("Authorization", "Bearer " + tokenAutre))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Vous n'avez pas accès à cet abonnement"));
    }

    @Test
    void abonnementDUnAutre_suppression_403() throws Exception {
        String tokenProprietaire = inscrire("proprio.abo.suppr@example.com");
        String tokenAutre = inscrire("autre.abo.suppr@example.com");
        long abonnementId = creerAbonnement(tokenProprietaire);

        mockMvc.perform(delete("/api/abonnements/" + abonnementId).header("Authorization", "Bearer " + tokenAutre))
            .andExpect(status().isForbidden());

        // Le propriétaire, lui, y a toujours accès : l'abonnement n'a pas été supprimé
        mockMvc.perform(get("/api/abonnements/" + abonnementId).header("Authorization", "Bearer " + tokenProprietaire))
            .andExpect(status().isOk());
    }

    @Test
    void sonPropreAbonnement_lecture_200() throws Exception {
        String token = inscrire("proprio.abo.ok@example.com");
        long abonnementId = creerAbonnement(token);

        mockMvc.perform(get("/api/abonnements/" + abonnementId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(abonnementId));
    }

    @Test
    void commentaireDUnAutre_suppression_403AvecCorpsJson() throws Exception {
        Long commentaireId = creerCommentaireDeLAdmin();
        String token = inscrire("autre.commentaire@example.com");

        mockMvc.perform(delete("/api/commentaires/" + commentaireId).header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Seul l'auteur du commentaire ou un admin peut le supprimer"));
    }

    @Test
    void refusParRole_garde_leMessageGenerique() throws Exception {
        String token = inscrire("lecteur.role@example.com");

        mockMvc.perform(get("/api/utilisateurs").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Accès refusé : rôle insuffisant"));
    }

    private long creerAbonnement(String token) throws Exception {
        String body = mockMvc.perform(post("/api/abonnements")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "type", "mensuel",
                    "montant", 5,
                    "dateDebut", LocalDate.now().toString(),
                    "dateFin", LocalDate.now().plusMonths(1).toString()))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long creerCommentaireDeLAdmin() {
        Article article = new Article();
        article.setTitre("Article commenté");
        article.setStatut(statutRepository.findByNomIgnoreCase("publie").orElseThrow());
        article = articleRepository.save(article);

        Utilisateur admin = utilisateurRepository.findByEmail("admin@echomigrant.local").orElseThrow();
        Commentaire commentaire = new Commentaire();
        commentaire.setContenu("Commentaire de l'admin");
        commentaire.setArticle(article);
        commentaire.setUtilisateur(admin);
        return commentaireRepository.save(commentaire).getId();
    }

    private String inscrire(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                    "nom", "Test", "prenom", "Utilisateur", "email", email, "password", "motdepasse123"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
