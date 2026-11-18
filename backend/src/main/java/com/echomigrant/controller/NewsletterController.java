package com.echomigrant.controller;

import com.echomigrant.dto.InscriptionNewsletterDto;
import com.echomigrant.dto.NewsletterRequest;
import com.echomigrant.service.NewsletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    /** Public (route ouverte dans SecurityConfig). Toujours 200, que l'email soit nouveau ou déjà inscrit. */
    @PostMapping
    public ResponseEntity<Map<String, String>> inscrire(@Valid @RequestBody NewsletterRequest request) {
        newsletterService.inscrire(request.email());
        return ResponseEntity.ok(Map.of("message", "Inscription à la newsletter enregistrée"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<InscriptionNewsletterDto> findAll() {
        return newsletterService.findAll();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsletterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
