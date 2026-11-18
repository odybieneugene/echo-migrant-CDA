package com.echomigrant.controller;

import com.echomigrant.dto.simple.NomDescriptionRequest;
import com.echomigrant.dto.simple.NomDescriptionResponse;
import com.echomigrant.entity.TypeArticle;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.TypeArticleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/types-articles")
@RequiredArgsConstructor
public class TypeArticleController {

    private final TypeArticleRepository typeArticleRepository;

    @GetMapping
    public List<NomDescriptionResponse> index() {
        return typeArticleRepository.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public NomDescriptionResponse show(@PathVariable Long id) {
        return toDto(find(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE')")
    public ResponseEntity<NomDescriptionResponse> store(@Valid @RequestBody NomDescriptionRequest request) {
        TypeArticle entity = new TypeArticle();
        entity.setNom(request.nom());
        entity.setDescription(request.description());
        entity = typeArticleRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE')")
    public NomDescriptionResponse update(@PathVariable Long id, @Valid @RequestBody NomDescriptionRequest request) {
        TypeArticle entity = find(id);
        entity.setNom(request.nom());
        entity.setDescription(request.description());
        return toDto(typeArticleRepository.save(entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> destroy(@PathVariable Long id) {
        typeArticleRepository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private TypeArticle find(Long id) {
        return typeArticleRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("TypeArticle", id));
    }

    private NomDescriptionResponse toDto(TypeArticle e) {
        return new NomDescriptionResponse(e.getId(), e.getNom(), e.getDescription());
    }
}
