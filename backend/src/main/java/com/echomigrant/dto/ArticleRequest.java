package com.echomigrant.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ArticleRequest(
    @NotBlank(message = "Le titre est obligatoire") String titre,
    String resume,
    String contenu,
    Long categorieId,
    Long typeArticleId,
    Long formatId,
    Long statutId,
    String metaTitre,
    String metaDescription,
    String metaKeywords,
    Boolean estEnVedette,
    List<Long> tagSeoIds
) {
}
