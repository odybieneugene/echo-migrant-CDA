package com.echomigrant.dto;

import com.echomigrant.entity.Role;
import com.echomigrant.entity.Utilisateur;

import java.util.Set;
import java.util.stream.Collectors;

public record UtilisateurDto(
    Long id,
    String nom,
    String prenom,
    String email,
    Set<String> roles
) {
    public static UtilisateurDto from(Utilisateur u) {
        return new UtilisateurDto(
            u.getId(),
            u.getNom(),
            u.getPrenom(),
            u.getEmail(),
            u.getRoles().stream().map(Role::getNom).collect(Collectors.toSet())
        );
    }
}
