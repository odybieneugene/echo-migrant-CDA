package com.echomigrant.controller;

import com.echomigrant.dto.simple.NomDescriptionResponse;
import com.echomigrant.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;

    // Les rôles sont fixes (admin, journaliste, redacteur, lecteur) et posés par les migrations :
    // seule la lecture est exposée, réservée aux utilisateurs authentifiés (utile pour les formulaires admin).
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<NomDescriptionResponse> index() {
        return roleRepository.findAll().stream()
            .map(r -> new NomDescriptionResponse(r.getId(), r.getNom(), r.getDescription()))
            .toList();
    }
}
