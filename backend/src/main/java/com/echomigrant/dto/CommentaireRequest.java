package com.echomigrant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentaireRequest(
    @NotBlank(message = "Le commentaire ne peut pas être vide") String contenu,
    @NotNull(message = "L'article est obligatoire") Long articleId
) {
}
