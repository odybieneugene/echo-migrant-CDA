package com.echomigrant.controller;

import com.echomigrant.dto.simple.NomDescriptionRequest;
import com.echomigrant.dto.simple.NomDescriptionResponse;
import com.echomigrant.entity.Format;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.FormatRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/formats")
@RequiredArgsConstructor
public class FormatController {

    private final FormatRepository formatRepository;

    @GetMapping
    public List<NomDescriptionResponse> index() {
        return formatRepository.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public NomDescriptionResponse show(@PathVariable Long id) {
        return toDto(find(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE')")
    public ResponseEntity<NomDescriptionResponse> store(@Valid @RequestBody NomDescriptionRequest request) {
        Format entity = new Format();
        entity.setNom(request.nom());
        entity.setDescription(request.description());
        entity = formatRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE')")
    public NomDescriptionResponse update(@PathVariable Long id, @Valid @RequestBody NomDescriptionRequest request) {
        Format entity = find(id);
        entity.setNom(request.nom());
        entity.setDescription(request.description());
        return toDto(formatRepository.save(entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> destroy(@PathVariable Long id) {
        formatRepository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private Format find(Long id) {
        return formatRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Format", id));
    }

    private NomDescriptionResponse toDto(Format e) {
        return new NomDescriptionResponse(e.getId(), e.getNom(), e.getDescription());
    }
}
