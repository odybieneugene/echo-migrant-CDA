package com.echomigrant.controller;

import com.echomigrant.dto.RolesRequest;
import com.echomigrant.dto.UtilisateurDto;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// La création de compte passe par POST /api/auth/register (rôle lecteur, mot de passe haché, JWT).
// Ce contrôleur est l'administration des comptes : consultation, attribution des rôles, suppression.
@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    public List<UtilisateurDto> index() {
        return utilisateurService.findAll();
    }

    @GetMapping("/{id}")
    public UtilisateurDto show(@PathVariable Long id) {
        return utilisateurService.findById(id);
    }

    // PUT /api/utilisateurs/{id}/roles  {"roles": ["journaliste"]} -> remplace les rôles de l'utilisateur
    @PutMapping("/{id}/roles")
    public UtilisateurDto updateRoles(
        @PathVariable Long id,
        @Valid @RequestBody RolesRequest request,
        @AuthenticationPrincipal Utilisateur adminConnecte
    ) {
        return utilisateurService.updateRoles(id, request.roles(), adminConnecte);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> destroy(@PathVariable Long id, @AuthenticationPrincipal Utilisateur adminConnecte) {
        utilisateurService.delete(id, adminConnecte);
        return ResponseEntity.noContent().build();
    }
}
