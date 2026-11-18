package com.echomigrant.dto;

import com.echomigrant.dto.simple.NomDescriptionResponse;
import com.echomigrant.entity.Article;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleResponse(
    Long id,
    String titre,
    String resume,
    String contenu,
    String imageCouverture,
    String metaTitre,
    String metaDescription,
    String metaKeywords,
    boolean estEnVedette,
    NomDescriptionResponse categorie,
    NomDescriptionResponse typeArticle,
    NomDescriptionResponse format,
    NomDescriptionResponse statut,
    Long auteurId,
    String auteurNomComplet,
    List<TagSeoDto> tagsSeo,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ArticleResponse from(Article a) {
        return from(a, "");
    }

    /** uploadsBaseUrl : ex. "http://localhost:8081", pour obtenir une URL absolue utilisable par le frontend. */
    public static ArticleResponse from(Article a, String uploadsBaseUrl) {
        String image = a.getImageCouverture();
        String imageUrl = (image == null || image.isBlank()) ? null : uploadsBaseUrl + "/uploads/" + image;
        return new ArticleResponse(
            a.getId(),
            a.getTitre(),
            a.getResume(),
            a.getContenu(),
            imageUrl,
            a.getMetaTitre(),
            a.getMetaDescription(),
            a.getMetaKeywords(),
            a.isEstEnVedette(),
            a.getCategorie() == null ? null : new NomDescriptionResponse(a.getCategorie().getId(), a.getCategorie().getNom(), a.getCategorie().getDescription()),
            a.getTypeArticle() == null ? null : new NomDescriptionResponse(a.getTypeArticle().getId(), a.getTypeArticle().getNom(), a.getTypeArticle().getDescription()),
            a.getFormat() == null ? null : new NomDescriptionResponse(a.getFormat().getId(), a.getFormat().getNom(), a.getFormat().getDescription()),
            a.getStatut() == null ? null : new NomDescriptionResponse(a.getStatut().getId(), a.getStatut().getNom(), a.getStatut().getDescription()),
            a.getUtilisateur() == null ? null : a.getUtilisateur().getId(),
            a.getUtilisateur() == null ? null : a.getUtilisateur().getPrenom() + " " + a.getUtilisateur().getNom(),
            a.getTagsSeo().stream().map(TagSeoDto::from).toList(),
            a.getCreatedAt(),
            a.getUpdatedAt()
        );
    }
}
