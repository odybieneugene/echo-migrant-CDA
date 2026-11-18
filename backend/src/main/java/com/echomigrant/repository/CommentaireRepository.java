package com.echomigrant.repository;

import com.echomigrant.entity.Commentaire;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    // L'auteur est chargé dans la même requête : CommentaireDto lit son nom hors transaction
    // (open-in-view désactivé), ce qui provoquerait sinon une LazyInitializationException.
    @EntityGraph(attributePaths = "utilisateur")
    List<Commentaire> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    @EntityGraph(attributePaths = "utilisateur")
    List<Commentaire> findAllByOrderByCreatedAtDesc();
}
