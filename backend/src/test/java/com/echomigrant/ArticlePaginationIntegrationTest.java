package com.echomigrant;

import com.echomigrant.entity.Categorie;
import com.echomigrant.repository.CategorieRepository;
import com.echomigrant.repository.StatutRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GET /api/articles/recherche : articles publiés, page par page, filtrés par catégorie et par recherche.
 * La base H2 est partagée entre classes : chaque test cherche un mot unique (jeton) présent seulement dans ses articles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArticlePaginationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StatutRepository statutRepository;
    @Autowired private CategorieRepository categorieRepository;

    private String token;
    private String jeton;

    @BeforeEach
    void preparer() throws Exception {
        token = connecterAdmin();
        // Lettres seulement : le jeton doit rester un « mot » de la recherche
        jeton = "jeton" + UUID.randomUUID().toString().replaceAll("[^a-f]", "");
    }

    @Test
    void decoupeLesArticlesPubliesEnPages_duPlusRecentAuPlusAncien() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ids.add(creerArticle("Article " + i + " " + jeton, "publie", null));
        }

        JsonNode page0 = rechercher("q=" + jeton + "&page=0&taille=2");
        assertThat(page0.get("totalElements").asLong()).isEqualTo(5);
        assertThat(page0.get("totalPages").asInt()).isEqualTo(3);
        assertThat(page0.get("page").asInt()).isZero();
        assertThat(page0.get("taille").asInt()).isEqualTo(2);
        assertThat(idsDe(page0)).containsExactly(ids.get(4), ids.get(3));

        JsonNode page2 = rechercher("q=" + jeton + "&page=2&taille=2");
        assertThat(idsDe(page2)).containsExactly(ids.get(0));

        // Au-delà de la dernière page : liste vide, pas d'erreur
        assertThat(idsDe(rechercher("q=" + jeton + "&page=9&taille=2"))).isEmpty();
    }

    @Test
    void lesBrouillonsNeSontPasDansLaListe() throws Exception {
        long publie = creerArticle("Publié " + jeton, "publie", null);
        creerArticle("Brouillon " + jeton, "brouillon", null);

        JsonNode page = rechercher("q=" + jeton);
        assertThat(idsDe(page)).containsExactly(publie);
        assertThat(page.get("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void rechercheSansAccentsNiMajuscules_tousLesMotsDansLeTitreOuLeResume() throws Exception {
        long societe = creerArticle("Société et migrations " + jeton, "publie", null);
        long autre = creerArticle("Autre sujet " + jeton, "publie", null);

        assertThat(idsDe(rechercher("q=SOCIETE " + jeton))).containsExactly(societe);
        assertThat(idsDe(rechercher("q=" + jeton + " migration"))).containsExactly(societe);
        assertThat(idsDe(rechercher("q=" + jeton))).containsExactlyInAnyOrder(societe, autre);
    }

    @Test
    void lesCaracteresSpeciauxDeLikeSontCherchesTelsQuels() throws Exception {
        creerArticle("Article ordinaire " + jeton, "publie", null);

        // Sans échappement, « % » et « _ » seraient des jokers et trouveraient l'article
        assertThat(idsDe(rechercher("q=" + jeton + " %25"))).isEmpty();
        assertThat(idsDe(rechercher("q=" + jeton + " _"))).isEmpty();
    }

    @Test
    void filtreParCategorie() throws Exception {
        Categorie categorie = new Categorie();
        categorie.setNom("Catégorie " + jeton);
        categorie = categorieRepository.save(categorie);
        long dedans = creerArticle("Dans la catégorie " + jeton, "publie", categorie.getId());
        creerArticle("Hors catégorie " + jeton, "publie", null);

        assertThat(idsDe(rechercher("categorie=" + categorie.getId() + "&q=" + jeton))).containsExactly(dedans);
    }

    @Test
    void parametresInvalides_400() throws Exception {
        mockMvc.perform(get("/api/articles/recherche?page=-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/articles/recherche?taille=0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/articles/recherche?taille=51")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/articles/recherche?categorie=abc")).andExpect(status().isBadRequest());
    }

    @Test
    void accessibleSansEtreConnecte_avecUneTailleParDefaut() throws Exception {
        String body = mockMvc.perform(get("/api/articles/recherche"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(body).get("taille").asInt()).isEqualTo(12);
    }

    private JsonNode rechercher(String params) throws Exception {
        // URI.create : la chaîne est déjà encodée, MockMvc ne doit pas la ré-encoder (%20 -> %2520)
        String body = mockMvc.perform(get(URI.create("/api/articles/recherche?" + params.replace(" ", "%20"))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private static List<Long> idsDe(JsonNode page) {
        List<Long> ids = new ArrayList<>();
        page.get("contenu").forEach(a -> ids.add(a.get("id").asLong()));
        return ids;
    }

    private long creerArticle(String titre, String statut, Long categorieId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("titre", titre);
        data.put("statutId", statutRepository.findByNomIgnoreCase(statut).orElseThrow().getId());
        if (categorieId != null) {
            data.put("categorieId", categorieId);
        }
        String body = mockMvc.perform(multipart("/api/articles")
                .file(new MockMultipartFile("data", "", "application/json", objectMapper.writeValueAsBytes(data)))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
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
