package com.echomigrant.controller;

import com.echomigrant.dto.DonDto;
import com.echomigrant.dto.DonRequest;
import com.echomigrant.entity.Don;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.DonRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dons")
@RequiredArgsConstructor
public class DonController {

    private final DonRepository donRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<DonDto> index() {
        return donRepository.findAll().stream().map(DonDto::from).toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonDto> store(
        @Valid @RequestBody DonRequest request,
        @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        Don don = new Don();
        don.setMontant(request.montant());
        don.setMoyenPaiement(request.moyenPaiement());
        don.setUtilisateur(utilisateur);
        don = donRepository.save(don);
        return ResponseEntity.status(HttpStatus.CREATED).body(DonDto.from(don));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DonDto show(@PathVariable Long id) {
        return DonDto.from(donRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Don", id)));
    }
}
