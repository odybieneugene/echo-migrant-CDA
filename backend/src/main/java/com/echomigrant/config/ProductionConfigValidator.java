package com.echomigrant.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * En production uniquement : refuse de démarrer si un secret est faible ou reste à sa valeur de
 * développement (publiée sur GitHub). Mieux vaut une application qui ne démarre pas qu'une
 * application où n'importe qui peut signer un jeton admin avec la clé connue.
 */
@Component
@Profile("prod")
@Slf4j
public class ProductionConfigValidator {

    static final int LONGUEUR_MIN_SECRET_JWT = 32;   // HMAC-SHA256 : au moins 256 bits
    static final int LONGUEUR_MIN_MOT_DE_PASSE_ADMIN = 12;
    private static final List<String> MARQUEURS_DEV = List.of("changeme", "change-me", "dev-only", "not-for-production");
    private static final List<String> MOTS_DE_PASSE_CONNUS = List.of("ChangeMoi123!", "admin", "password", "echo_migrant");

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.admin.default-password}")
    private String adminPassword;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @PostConstruct
    void verifierAuDemarrage() {
        List<String> problemes = verifier(jwtSecret, adminPassword, dbPassword);
        if (!problemes.isEmpty()) {
            throw new IllegalStateException("Configuration de production refusée :\n - " + String.join("\n - ", problemes));
        }
        log.info("Configuration de production vérifiée : secrets présents et non triviaux");
    }

    /** Liste tous les problèmes d'un coup (plutôt que de les découvrir un par un à chaque redémarrage). */
    static List<String> verifier(String jwtSecret, String adminPassword, String dbPassword) {
        List<String> problemes = new ArrayList<>();

        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < LONGUEUR_MIN_SECRET_JWT) {
            problemes.add("JWT_SECRET trop court (" + LONGUEUR_MIN_SECRET_JWT + " caractères minimum, ex. : openssl rand -base64 48)");
        }
        String secretMinuscule = jwtSecret.toLowerCase(Locale.ROOT);
        if (MARQUEURS_DEV.stream().anyMatch(secretMinuscule::contains)) {
            problemes.add("JWT_SECRET est une valeur de développement : générer un secret aléatoire");
        }

        if (adminPassword.length() < LONGUEUR_MIN_MOT_DE_PASSE_ADMIN) {
            problemes.add("ADMIN_DEFAULT_PASSWORD trop court (" + LONGUEUR_MIN_MOT_DE_PASSE_ADMIN + " caractères minimum)");
        }
        if (MOTS_DE_PASSE_CONNUS.contains(adminPassword)) {
            problemes.add("ADMIN_DEFAULT_PASSWORD est un mot de passe connu (valeur de développement)");
        }

        if (MOTS_DE_PASSE_CONNUS.contains(dbPassword)) {
            problemes.add("DB_PASSWORD est un mot de passe connu (valeur de développement)");
        }
        return problemes;
    }
}
