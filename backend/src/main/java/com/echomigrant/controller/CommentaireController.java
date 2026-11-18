package com.echomigrant.controller;

import com.echomigrant.dto.CommentaireDto;
import com.echomigrant.dto.CommentaireRequest;
import com.echomigrant.entity.Article;
import com.echomigrant.entity.Commentaire;
import com.echomigrant.entity.Role;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.CommentaireRepository;
import com.echomigrant.service.ArticleService;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commentaires")
@RequiredArgsConstructor
public class CommentaireController {

    private final CommentaireRepository commentaireRepository;
    private final ArticleService articleService;

    // GET /api/commentaires?articleId=X -> public, si l'article est visible (pas un brouillon pour un visiteur)
    // GET /api/commentaires             -> tous les commentaires du site : réservé à l'admin (modération)
    @GetMapping
    public List<CommentaireDto> index(
        @RequestParam(required = false) Long articleId,
        @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        List<Commentaire> commentaires;
        if (articleId != null) {
            articleService.getVisibleOrThrow(articleId, utilisateur);
            commentaires = commentaireRepository.findByArticleIdOrderByCreatedAtDesc(articleId);
        } else {
            if (utilisateur == null || !utilisateur.hasRole(Role.ADMIN)) {
                // Traduit en 401 (anonyme) ou 403 (connecté) par GlobalExceptionHandler
                throw new AccessDeniedException("Liste complète des commentaires réservée à l'admin");
            }
            commentaires = commentaireRepository.findAllByOrderByCreatedAtDesc();
        }
        return commentaires.stream().map(CommentaireDto::from).toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentaireDto> store(
        @Valid @RequestBody CommentaireRequest request,
        @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        // On ne peut commenter qu'un article visible (pas un brouillon deviné par son id)
        Article article = articleService.getVisibleOrThrow(request.articleId(), utilisateur);

        Commentaire commentaire = new Commentaire();
        commentaire.setContenu(request.contenu());
        commentaire.setArticle(article);
        commentaire.setUtilisateur(utilisateur);
        commentaire = commentaireRepository.save(commentaire);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommentaireDto.from(commentaire));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> destroy(@PathVariable Long id, @AuthenticationPrincipal Utilisateur utilisateur) {
        Commentaire commentaire = commentaireRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Commentaire", id));

        boolean isOwner = commentaire.getUtilisateur().getId().equals(utilisateur.getId());
        boolean isAdmin = utilisateur.hasRole("admin");
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(403).build();
        }

        commentaireRepository.delete(commentaire);
        return ResponseEntity.noContent().build();
    }
}
