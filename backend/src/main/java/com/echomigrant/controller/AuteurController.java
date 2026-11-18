package com.echomigrant.controller;

import com.echomigrant.dto.AuteurDto;
import com.echomigrant.entity.Auteur;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.AuteurRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auteurs")
@RequiredArgsConstructor
public class AuteurController {

    private final AuteurRepository auteurRepository;

    @GetMapping
    public List<AuteurDto> index() {
        return auteurRepository.findAll().stream().map(AuteurDto::from).toList();
    }

    @GetMapping("/{id}")
    public AuteurDto show(@PathVariable Long id) {
        return AuteurDto.from(find(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE')")
    public ResponseEntity<AuteurDto> store(@Valid @RequestBody AuteurDto dto) {
        Auteur entity = new Auteur();
        entity.setNom(dto.nom());
        entity.setPrenom(dto.prenom());
        entity.setEmail(dto.email());
        entity.setBio(dto.bio());
        entity = auteurRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(AuteurDto.from(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE')")
    public AuteurDto update(@PathVariable Long id, @Valid @RequestBody AuteurDto dto) {
        Auteur entity = find(id);
        entity.setNom(dto.nom());
        entity.setPrenom(dto.prenom());
        entity.setEmail(dto.email());
        entity.setBio(dto.bio());
        return AuteurDto.from(auteurRepository.save(entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> destroy(@PathVariable Long id) {
        auteurRepository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private Auteur find(Long id) {
        return auteurRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Auteur", id));
    }
}
