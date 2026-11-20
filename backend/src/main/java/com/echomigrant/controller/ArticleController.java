package com.echomigrant.controller;

import com.echomigrant.dto.ArticleRequest;
import com.echomigrant.dto.ArticleResponse;
import com.echomigrant.dto.PageResponse;
import com.echomigrant.exception.BadRequestException;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    // GET /api/articles -> public, uniquement les articles publiés, triés du plus récent au plus ancien
    @GetMapping
    public List<ArticleResponse> index(@RequestParam(required = false) Integer limit) {
        return articleService.findPublies(limit);
    }

    /** Taille de page maximale : un client ne peut pas redemander toute la table d'un coup. */
    private static final int TAILLE_MAX = 50;

    // GET /api/articles/recherche?categorie=3&q=asile&page=0&taille=12 -> public, articles publiés page par page
    @GetMapping("/recherche")
    public PageResponse<ArticleResponse> rechercher(@RequestParam(required = false) Long categorie,
                                                    @RequestParam(required = false) String q,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "12") int taille) {
        if (page < 0) {
            throw new BadRequestException("Le numéro de page commence à 0");
        }
        if (taille < 1 || taille > TAILLE_MAX) {
            throw new BadRequestException("La taille de page doit être comprise entre 1 et " + TAILLE_MAX);
        }
        return articleService.rechercherPublies(categorie, q, page, taille);
    }

    // GET /api/articles/admin -> dashboard : tous les articles (brouillons + publiés)
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE','REDACTEUR')")
    public List<ArticleResponse> indexAdmin() {
        return articleService.findAllAdmin();
    }

    @GetMapping("/{id}")
    public ArticleResponse show(@PathVariable Long id, @AuthenticationPrincipal Utilisateur utilisateur) {
        return articleService.findVisibleById(id, utilisateur);
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE','REDACTEUR')")
    public ResponseEntity<ArticleResponse> store(
        @Valid @RequestPart("data") ArticleRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image,
        @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articleService.create(request, image, utilisateur));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE','REDACTEUR')")
    public ArticleResponse update(
        @PathVariable Long id,
        @Valid @RequestPart("data") ArticleRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image,
        @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        return articleService.update(id, request, image, utilisateur);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','JOURNALISTE')")
    public ResponseEntity<Void> destroy(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
