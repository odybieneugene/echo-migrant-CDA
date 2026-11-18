package com.echomigrant.dto;

import com.echomigrant.entity.Don;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DonDto(
    Long id,
    BigDecimal montant,
    String moyenPaiement,
    Long utilisateurId,
    LocalDateTime createdAt
) {
    public static DonDto from(Don d) {
        return new DonDto(d.getId(), d.getMontant(), d.getMoyenPaiement(), d.getUtilisateur().getId(), d.getCreatedAt());
    }
}
