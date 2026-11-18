package com.echomigrant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewsletterRequest(
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    @Size(max = 255, message = "Email trop long") String email
) {
    /** Espaces autour retirés avant la validation (copier-coller fréquent). */
    public NewsletterRequest {
        email = email == null ? null : email.strip();
    }
}
