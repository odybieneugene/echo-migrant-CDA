package com.echomigrant.dto;

import com.echomigrant.entity.Abonnement;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AbonnementDto(
    Long id,
    Long utilisateurId,
    String type,
    BigDecimal montant,
    LocalDate dateDebut,
    LocalDate dateFin,
    String statut
) {
    public static AbonnementDto from(Abonnement a) {
        return new AbonnementDto(
            a.getId(),
            a.getUtilisateur().getId(),
            a.getType().name(),
            a.getMontant(),
            a.getDateDebut(),
            a.getDateFin(),
            a.getStatut()
        );
    }
}
