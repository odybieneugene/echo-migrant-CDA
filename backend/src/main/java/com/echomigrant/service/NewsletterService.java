package com.echomigrant.service;

import com.echomigrant.dto.InscriptionNewsletterDto;
import com.echomigrant.entity.InscriptionNewsletter;
import com.echomigrant.exception.ResourceNotFoundException;
import com.echomigrant.repository.InscriptionNewsletterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final InscriptionNewsletterRepository repository;

    /**
     * Inscription idempotente : un email déjà inscrit ne crée pas de doublon et la réponse est la même
     * que pour une nouvelle inscription — un visiteur ne peut donc pas tester si un email est déjà dans la liste.
     */
    public void inscrire(String email) {
        String normalise = email.trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmail(normalise)) {
            return;
        }
        try {
            repository.save(new InscriptionNewsletter(normalise));
        } catch (DataIntegrityViolationException e) {
            // Deux requêtes simultanées avec le même email : la contrainte UNIQUE a joué, l'email est bien inscrit.
        }
    }

    @Transactional(readOnly = true)
    public List<InscriptionNewsletterDto> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(InscriptionNewsletterDto::from).toList();
    }

    /** Désinscription par l'admin (ex. demande d'effacement RGPD reçue par email). */
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Inscription introuvable");
        }
        repository.deleteById(id);
    }
}
