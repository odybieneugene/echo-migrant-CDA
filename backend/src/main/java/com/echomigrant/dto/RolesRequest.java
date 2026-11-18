package com.echomigrant.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/** Nouveaux rôles d'un utilisateur (remplacent les anciens), ex. {"roles": ["journaliste"]}. */
public record RolesRequest(
    @NotEmpty(message = "Au moins un rôle est obligatoire") Set<String> roles
) {
}
