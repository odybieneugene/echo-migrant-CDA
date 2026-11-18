package com.echomigrant.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangementMotDePasseRequest(
    @NotBlank(message = "Le mot de passe actuel est obligatoire") String motDePasseActuel,
    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères") String nouveauMotDePasse
) {}
