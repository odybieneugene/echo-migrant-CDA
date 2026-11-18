package com.echomigrant.repository;

import com.echomigrant.entity.InscriptionNewsletter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscriptionNewsletterRepository extends JpaRepository<InscriptionNewsletter, Long> {

    boolean existsByEmail(String email);

    List<InscriptionNewsletter> findAllByOrderByCreatedAtDesc();
}
