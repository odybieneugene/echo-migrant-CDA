package com.echomigrant.repository;

import com.echomigrant.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);

    // Requête "exists" en base, au lieu de charger tous les utilisateurs et leurs rôles
    boolean existsByRoles_NomIgnoreCase(String nomRole);

    long countByRoles_NomIgnoreCase(String nomRole);
}
