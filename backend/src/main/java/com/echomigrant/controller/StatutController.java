package com.echomigrant.controller;

import com.echomigrant.dto.simple.NomDescriptionRequest;
import com.echomigrant.dto.simple.NomDescriptionResponse;
import com.echomigrant.entity.Statut;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.StatutRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statuses")
@RequiredArgsConstructor
public class StatutController {

    private final StatutRepository statutRepository;

    @GetMapping
    public List<NomDescriptionResponse> index() {
        return statutRepository.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public NomDescriptionResponse show(@PathVariable Long id) {
        return toDto(find(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NomDescriptionResponse> store(@Valid @RequestBody NomDescriptionRequest request) {
        Statut entity = new Statut();
        entity.setNom(request.nom());
        entity.setDescription(request.description());
        entity = statutRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NomDescriptionResponse update(@PathVariable Long id, @Valid @RequestBody NomDescriptionRequest request) {
        Statut entity = find(id);
        entity.setNom(request.nom());
        entity.setDescription(request.description());
        return toDto(statutRepository.save(entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> destroy(@PathVariable Long id) {
        statutRepository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private Statut find(Long id) {
        return statutRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Statut", id));
    }

    private NomDescriptionResponse toDto(Statut e) {
        return new NomDescriptionResponse(e.getId(), e.getNom(), e.getDescription());
    }
}
