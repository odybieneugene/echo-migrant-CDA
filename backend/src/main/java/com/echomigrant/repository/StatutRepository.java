package com.echomigrant.repository;

import com.echomigrant.entity.Statut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatutRepository extends JpaRepository<Statut, Long> {
    Optional<Statut> findByNomIgnoreCase(String nom);
}
