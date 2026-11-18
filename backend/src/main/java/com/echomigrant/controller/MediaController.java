package com.echomigrant.controller;

import com.echomigrant.dto.MediaDto;
import com.echomigrant.entity.Media;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.MediaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medias")
@RequiredArgsConstructor
public class MediaController {

    private final MediaRepository mediaRepository;

    @GetMapping
    public List<MediaDto> index() {
        return mediaRepository.findAll().stream().map(MediaDto::from).toList();
    }

    @GetMapping("/{id}")
    public MediaDto show(@PathVariable Long id) {
        return MediaDto.from(find(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE','REDACTEUR')")
    public ResponseEntity<MediaDto> store(@Valid @RequestBody MediaDto dto) {
        Media entity = new Media();
        entity.setType(dto.type());
        entity.setUrl(dto.url());
        entity.setDescription(dto.description());
        entity = mediaRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(MediaDto.from(entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE')")
    public ResponseEntity<Void> destroy(@PathVariable Long id) {
        mediaRepository.delete(find(id));
        return ResponseEntity.noContent().build();
    }

    private Media find(Long id) {
        return mediaRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Media", id));
    }
}
