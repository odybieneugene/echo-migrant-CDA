package com.echomigrant.service;

import com.echomigrant.dto.ArticleRequest;
import com.echomigrant.dto.ArticleResponse;
import com.echomigrant.entity.*;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategorieRepository categorieRepository;
    private final TypeArticleRepository typeArticleRepository;
    private final FormatRepository formatRepository;
    private final StatutRepository statutRepository;
    private final TagSeoRepository tagSeoRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.uploads.base-url:http://localhost:8081}")
    private String uploadsBaseUrl;

    @Transactional(readOnly = true)
    public List<ArticleResponse> findPublies(Integer limit) {
        Pageable pageable = limit != null ? PageRequest.of(0, limit) : Pageable.unpaged();
        return articleRepository.findAllPublies(pageable).stream().map(a -> ArticleResponse.from(a, uploadsBaseUrl)).toList();
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> findAllAdmin() {
        return articleRepository.findAllByOrderByCreatedAtDesc().stream().map(a -> ArticleResponse.from(a, uploadsBaseUrl)).toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse findById(Long id) {
        return ArticleResponse.from(getOrThrow(id), uploadsBaseUrl);
    }

    @Transactional
    public ArticleResponse create(ArticleRequest request, MultipartFile image, Utilisateur auteur) {
        Article article = new Article();
        applyRequest(article, request);
        article.setUtilisateur(auteur);

        if (image != null && !image.isEmpty()) {
            article.setImageCouverture(fileStorageService.storeImage(image));
        }

        if (Boolean.TRUE.equals(request.estEnVedette())) {
            articleRepository.findAll().stream()
                .filter(Article::isEstEnVedette)
                .forEach(a -> a.setEstEnVedette(false));
        }

        return ArticleResponse.from(articleRepository.save(article), uploadsBaseUrl);
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleRequest request, MultipartFile image) {
        Article article = getOrThrow(id);
        String ancienneImage = article.getImageCouverture();

        applyRequest(article, request);

        if (image != null && !image.isEmpty()) {
            article.setImageCouverture(fileStorageService.storeImage(image));
            fileStorageService.deleteImage(ancienneImage);
        }

        if (Boolean.TRUE.equals(request.estEnVedette())) {
            articleRepository.findAll().stream()
                .filter(a -> !a.getId().equals(id) && a.isEstEnVedette())
                .forEach(a -> a.setEstEnVedette(false));
        }

        return ArticleResponse.from(articleRepository.save(article), uploadsBaseUrl);
    }

    @Transactional
    public void delete(Long id) {
        Article article = getOrThrow(id);
        fileStorageService.deleteImage(article.getImageCouverture());
        articleRepository.delete(article);
    }

    private void applyRequest(Article article, ArticleRequest request) {
        article.setTitre(request.titre());
        article.setResume(request.resume());
        article.setContenu(request.contenu());
        article.setMetaTitre(request.metaTitre());
        article.setMetaDescription(request.metaDescription());
        article.setMetaKeywords(request.metaKeywords());
        if (request.estEnVedette() != null) {
            article.setEstEnVedette(request.estEnVedette());
        }

        if (request.categorieId() != null) {
            article.setCategorie(categorieRepository.findById(request.categorieId())
                .orElseThrow(() -> ResourceNotFoundException.of("Categorie", request.categorieId())));
        } else {
            article.setCategorie(null);
        }

        if (request.typeArticleId() != null) {
            article.setTypeArticle(typeArticleRepository.findById(request.typeArticleId())
                .orElseThrow(() -> ResourceNotFoundException.of("TypeArticle", request.typeArticleId())));
        } else {
            article.setTypeArticle(null);
        }

        if (request.formatId() != null) {
            article.setFormat(formatRepository.findById(request.formatId())
                .orElseThrow(() -> ResourceNotFoundException.of("Format", request.formatId())));
        } else {
            article.setFormat(null);
        }

        if (request.statutId() != null) {
            article.setStatut(statutRepository.findById(request.statutId())
                .orElseThrow(() -> ResourceNotFoundException.of("Statut", request.statutId())));
        }

        if (request.tagSeoIds() != null) {
            Set<TagSeo> tags = new HashSet<>(tagSeoRepository.findAllById(request.tagSeoIds()));
            article.setTagsSeo(tags);
        }
    }

    private Article getOrThrow(Long id) {
        return articleRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.of("Article", id));
    }
}
