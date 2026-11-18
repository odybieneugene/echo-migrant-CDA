package com.echomigrant.controller;

import com.echomigrant.dto.UtilisateurDto;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// La création de compte passe par POST /api/auth/register (avec hachage du mot de passe + JWT).
// Ce contrôleur gère uniquement la consultation / suppression par un administrateur.
@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UtilisateurDto> index() {
        return utilisateurRepository.findAll().stream().map(UtilisateurDto::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UtilisateurDto show(@PathVariable Long id) {
        return UtilisateurDto.from(find(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> destroy(@PathVariable Long id) {
        utilisateurRepository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private Utilisateur find(Long id) {
        return utilisateurRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Utilisateur", id));
    }
}
