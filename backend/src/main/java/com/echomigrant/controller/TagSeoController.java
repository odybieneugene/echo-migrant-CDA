package com.echomigrant.controller;

import com.echomigrant.dto.TagSeoDto;
import com.echomigrant.entity.TagSeo;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.TagSeoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags-seo")
@RequiredArgsConstructor
public class TagSeoController {

    private final TagSeoRepository tagSeoRepository;

    @GetMapping
    public List<TagSeoDto> index() {
        return tagSeoRepository.findAll().stream().map(TagSeoDto::from).toList();
    }

    @GetMapping("/{id}")
    public TagSeoDto show(@PathVariable Long id) {
        return TagSeoDto.from(find(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE','REDACTEUR')")
    public ResponseEntity<TagSeoDto> store(@Valid @RequestBody TagSeoDto dto) {
        TagSeo entity = new TagSeo();
        entity.setMotCle(dto.motCle());
        entity = tagSeoRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(TagSeoDto.from(entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> destroy(@PathVariable Long id) {
        tagSeoRepository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private TagSeo find(Long id) {
        return tagSeoRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("TagSeo", id));
    }
}
