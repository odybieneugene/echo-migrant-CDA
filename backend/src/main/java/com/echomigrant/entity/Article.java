package com.echomigrant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(name = "meta_titre")
    private String metaTitre;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "meta_keywords")
    private String metaKeywords;

    @Column(columnDefinition = "TEXT")
    private String resume;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "image_couverture")
    private String imageCouverture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_article_id")
    private TypeArticle typeArticle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "format_id")
    private Format format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_id")
    private Statut statut;

    // Auteur principal (utilisateur connecté qui a créé l'article)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "est_en_vedette", nullable = false)
    private boolean estEnVedette = false;

    @JsonIgnore
    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Commentaire> commentaires = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "contenirs",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "media_id")
    )
    private Set<Media> medias = new HashSet<>();

    // Co-rédacteurs (en plus de l'auteur principal)
    @ManyToMany
    @JoinTable(
        name = "redigers",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "utilisateur_id")
    )
    private Set<Utilisateur> redacteurs = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "article_tag_seo",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_seo_id")
    )
    private Set<TagSeo> tagsSeo = new HashSet<>();

    // Abonnements qui donnent accès à cet article (paywall)
    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "acceders",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "abonnement_id")
    )
    private Set<Abonnement> abonnementsRequis = new HashSet<>();

    /** Titre + résumé normalisés (voir {@link #normaliser}), pour la recherche paginée. Calculé, jamais saisi. */
    @Column(name = "texte_recherche", nullable = false, columnDefinition = "TEXT")
    @Setter(AccessLevel.NONE)
    private String texteRecherche;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        majTexteRecherche();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        majTexteRecherche();
    }

    private void majTexteRecherche() {
        this.texteRecherche = normaliser(titre + " " + (resume == null ? "" : resume));
    }

    /** Minuscules et sans accents : « Société » devient « societe ». Même règle que la migration V7. */
    public static String normaliser(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
