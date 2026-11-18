package com.echomigrant.dto;

import com.echomigrant.entity.InscriptionNewsletter;

import java.time.LocalDateTime;

public record InscriptionNewsletterDto(Long id, String email, LocalDateTime createdAt) {

    public static InscriptionNewsletterDto from(InscriptionNewsletter inscription) {
        return new InscriptionNewsletterDto(inscription.getId(), inscription.getEmail(), inscription.getCreatedAt());
    }
}
