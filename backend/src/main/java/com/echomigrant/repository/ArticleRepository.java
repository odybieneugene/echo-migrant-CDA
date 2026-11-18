package com.echomigrant.repository;

import com.echomigrant.entity.Article;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query("select a from Article a where a.statut.nom = 'publie' order by a.createdAt desc")
    List<Article> findAllPublies(Pageable pageable);

    List<Article> findAllByOrderByCreatedAtDesc();
}
