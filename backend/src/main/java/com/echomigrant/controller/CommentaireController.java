package com.echomigrant.controller;

import com.echomigrant.dto.CommentaireDto;
import com.echomigrant.dto.CommentaireRequest;
import com.echomigrant.entity.Article;
import com.echomigrant.entity.Commentaire;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.ArticleRepository;
import com.echomigrant.repository.CommentaireRepository;
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
    private final ArticleRepository articleRepository;

    @GetMapping
    public List<CommentaireDto> index(@RequestParam(required = false) Long articleId) {
        List<Commentaire> commentaires = articleId != null
            ? commentaireRepository.findByArticleIdOrderByCreatedAtDesc(articleId)
            : commentaireRepository.findAll();
        return commentaires.stream().map(CommentaireDto::from).toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentaireDto> store(
        @Valid @RequestBody CommentaireRequest request,
        @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        Article article = articleRepository.findById(request.articleId())
            .orElseThrow(() -> ResourceNotFoundException.of("Article", request.articleId()));

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
