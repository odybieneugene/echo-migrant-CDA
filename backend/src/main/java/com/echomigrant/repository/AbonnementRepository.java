package com.echomigrant.repository;

import com.echomigrant.entity.Abonnement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {
    List<Abonnement> findByUtilisateurId(Long utilisateurId);
    List<Abonnement> findByUtilisateurIdAndDateFinGreaterThanEqual(Long utilisateurId, LocalDate date);
}
