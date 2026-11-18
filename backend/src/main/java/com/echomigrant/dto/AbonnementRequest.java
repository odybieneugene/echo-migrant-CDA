package com.echomigrant.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AbonnementRequest(
    @NotNull(message = "Le type d'abonnement est obligatoire") String type,
    BigDecimal montant,
    @NotNull(message = "La date de début est obligatoire") LocalDate dateDebut,
    LocalDate dateFin
) {
}
