package com.echomigrant.config;

import com.echomigrant.entity.Role;
import com.echomigrant.entity.Utilisateur;
import com.echomigrant.repository.RoleRepository;
import com.echomigrant.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Crée automatiquement le premier compte administrateur au démarrage si aucun n'existe encore.
 * Reprend une exigence identifiée dans le projet Laravel d'origine (jamais implémentée côté Laravel) :
 * garantir qu'il existe toujours un administrateur pour accéder au dashboard après un déploiement.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-email:admin@echomigrant.local}")
    private String defaultAdminEmail;

    @Value("${app.admin.default-password:ChangeMoi123!}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        boolean adminExists = utilisateurRepository.findAll().stream().anyMatch(u -> u.hasRole(Role.ADMIN));
        if (adminExists) {
            return;
        }

        Role adminRole = roleRepository.findByNomIgnoreCase(Role.ADMIN)
            .orElseThrow(() -> new IllegalStateException("Le rôle 'admin' doit exister (voir migration V2)"));

        Utilisateur admin = new Utilisateur();
        admin.setNom("Admin");
        admin.setPrenom("Echo Migrant");
        admin.setEmail(defaultAdminEmail);
        admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);

        utilisateurRepository.save(admin);
        log.warn("Aucun administrateur trouvé : compte admin créé automatiquement ({} / mot de passe par défaut à changer immédiatement)", defaultAdminEmail);
    }
}
