package com.echomigrant.dto.simple;

import jakarta.validation.constraints.NotBlank;

/** DTO générique réutilisé pour les référentiels simples (Categorie, TypeArticle, Format, Statut, Role). */
public record NomDescriptionRequest(
    @NotBlank(message = "Le nom est obligatoire") String nom,
    String description
) {
}
