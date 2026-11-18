package com.echomigrant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DonRequest(
    @NotNull(message = "Le montant est obligatoire") @DecimalMin(value = "0.01", message = "Le montant doit être positif") BigDecimal montant,
    @NotBlank(message = "Le moyen de paiement est obligatoire") String moyenPaiement
) {
}
