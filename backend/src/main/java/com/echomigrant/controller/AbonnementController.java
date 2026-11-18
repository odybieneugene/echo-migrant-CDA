package com.echomigrant.controller;

import com.echomigrant.dto.AbonnementDto;
import com.echomigrant.dto.AbonnementRequest;
import com.echomigrant.entity.Abonnement;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.exception.BadRequestException;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.AbonnementRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/abonnements")
@RequiredArgsConstructor
public class AbonnementController {

    private final AbonnementRepository abonnementRepository;

    // GET /api/abonnements -> les siens (utilisateur courant), ou tous pour un admin
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AbonnementDto> index(@AuthenticationPrincipal Utilisateur utilisateur) {
        List<Abonnement> abonnements = utilisateur.hasRole("admin")
            ? abonnementRepository.findAll()
            : abonnementRepository.findByUtilisateurId(utilisateur.getId());
        return abonnements.stream().map(AbonnementDto::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public AbonnementDto show(@PathVariable Long id, @AuthenticationPrincipal Utilisateur utilisateur) {
        Abonnement abonnement = find(id);
        ensureOwnerOrAdmin(abonnement, utilisateur);
        return AbonnementDto.from(abonnement);
    }

    // Un utilisateur souscrit un abonnement pour lui-même
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AbonnementDto> store(
        @Valid @RequestBody AbonnementRequest request,
        @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        boolean dejaActif = abonnementRepository
            .findByUtilisateurIdAndDateFinGreaterThanEqual(utilisateur.getId(), LocalDate.now())
            .stream().anyMatch(a -> "actif".equals(a.getStatut()) || "en_attente".equals(a.getStatut()));
        if (dejaActif) {
            throw new BadRequestException("Vous avez déjà un abonnement actif ou en attente");
        }

        Abonnement abonnement = new Abonnement();
        abonnement.setUtilisateur(utilisateur);
        try {
            abonnement.setType(Abonnement.Type.valueOf(request.type()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Type d'abonnement invalide : " + request.type());
        }
        abonnement.setMontant(request.montant());
        abonnement.setDateDebut(request.dateDebut());
        abonnement.setDateFin(request.dateFin());
        abonnement.setStatut("en_attente");

        abonnement = abonnementRepository.save(abonnement);
        return ResponseEntity.status(HttpStatus.CREATED).body(AbonnementDto.from(abonnement));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> destroy(@PathVariable Long id, @AuthenticationPrincipal Utilisateur utilisateur) {
        Abonnement abonnement = find(id);
        ensureOwnerOrAdmin(abonnement, utilisateur);
        abonnementRepository.delete(abonnement);
        return ResponseEntity.noContent().build();
    }

    private void ensureOwnerOrAdmin(Abonnement abonnement, Utilisateur utilisateur) {
        boolean isOwner = abonnement.getUtilisateur().getId().equals(utilisateur.getId());
        if (!isOwner && !utilisateur.hasRole("admin")) {
            throw new BadRequestException("Vous n'avez pas accès à cet abonnement");
        }
    }

    private Abonnement find(Long id) {
        return abonnementRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Abonnement", id));
    }
}
