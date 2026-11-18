package com.echomigrant.controller;

import com.echomigrant.dto.simple.NomDescriptionRequest;
import com.echomigrant.dto.simple.NomDescriptionResponse;
import com.echomigrant.entity.Categorie;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.CategorieRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategorieController {

    private final CategorieRepository categorieRepository;

    @GetMapping
    public List<NomDescriptionResponse> index() {
        return categorieRepository.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public NomDescriptionResponse show(@PathVariable Long id) {
        return toDto(find(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE')")
    public ResponseEntity<NomDescriptionResponse> store(@Valid @RequestBody NomDescriptionRequest request) {
        Categorie categorie = new Categorie();
        categorie.setNom(request.nom());
        categorie.setDescription(request.description());
        categorie = categorieRepository.save(categorie);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(categorie));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE')")
    public NomDescriptionResponse update(@PathVariable Long id, @Valid @RequestBody NomDescriptionRequest request) {
        Categorie categorie = find(id);
        categorie.setNom(request.nom());
        categorie.setDescription(request.description());
        return toDto(categorieRepository.save(categorie));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> destroy(@PathVariable Long id) {
        categorieRepository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private Categorie find(Long id) {
        return categorieRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Categorie", id));
    }

    private NomDescriptionResponse toDto(Categorie c) {
        return new NomDescriptionResponse(c.getId(), c.getNom(), c.getDescription());
    }
}
