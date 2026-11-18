package com.echomigrant.dto;

import com.echomigrant.entity.Auteur;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuteurDto(
    Long id,
    @NotBlank(message = "Le nom est obligatoire") String nom,
    String prenom,
    @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
    String bio
) {
    public static AuteurDto from(Auteur a) {
        return new AuteurDto(a.getId(), a.getNom(), a.getPrenom(), a.getEmail(), a.getBio());
    }
}
