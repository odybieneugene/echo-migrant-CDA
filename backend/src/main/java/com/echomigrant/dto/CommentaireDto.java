package com.echomigrant.dto;

import com.echomigrant.entity.Commentaire;

import java.time.LocalDateTime;

public record CommentaireDto(
    Long id,
    String contenu,
    Long articleId,
    Long utilisateurId,
    String utilisateurNomComplet,
    LocalDateTime createdAt
) {
    public static CommentaireDto from(Commentaire c) {
        return new CommentaireDto(
            c.getId(),
            c.getContenu(),
            c.getArticle().getId(),
            c.getUtilisateur().getId(),
            c.getUtilisateur().getPrenom() + " " + c.getUtilisateur().getNom(),
            c.getCreatedAt()
        );
    }
}
