package com.echomigrant.repository;

import com.echomigrant.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNomIgnoreCase(String nom);
}
