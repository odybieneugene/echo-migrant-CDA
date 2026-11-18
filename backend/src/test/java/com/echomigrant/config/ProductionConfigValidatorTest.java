package com.echomigrant.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Règles de refus des secrets en production (test unitaire, sans contexte Spring). */
class ProductionConfigValidatorTest {

    private static final String SECRET_SOLIDE = "q8Zt3vN0pLw7Rk2yXc5Hb9Ue1Js4Fd6Gm0Ai8Oe3Tr7";
    private static final String MDP_SOLIDE = "Tr0ub4dor&3-cheval";

    @Test
    void configurationSolide_aucunProbleme() {
        assertThat(ProductionConfigValidator.verifier(SECRET_SOLIDE, MDP_SOLIDE, "Xk29!pqLm#v7")).isEmpty();
    }

    @Test
    void valeursDeDeveloppement_toutesSignalees() {
        // Les valeurs réellement présentes dans application.yml / docker-compose.yml de développement
        List<String> problemes = ProductionConfigValidator.verifier(
            "changeme-super-secret-key-for-echo-migrant-dev-only-please-override-in-prod",
            "ChangeMoi123!",
            "echo_migrant");

        assertThat(problemes).hasSize(3).anyMatch(p -> p.startsWith("JWT_SECRET est une valeur de développement"))
            .anyMatch(p -> p.startsWith("ADMIN_DEFAULT_PASSWORD est un mot de passe connu"))
            .anyMatch(p -> p.startsWith("DB_PASSWORD est un mot de passe connu"));
    }

    @Test
    void retourChariotInvisible_refuse() {
        // Cas réel : secret généré avec openssl sous Windows, "\r" resté au milieu de la valeur
        List<String> problemes = ProductionConfigValidator.verifier(SECRET_SOLIDE, "cXqnN92W02Ym745f\rAa1!", "Xk29!pqLm#v7\r");

        assertThat(problemes).hasSize(2)
            .anyMatch(p -> p.startsWith("ADMIN_DEFAULT_PASSWORD contient un caractère invisible"))
            .anyMatch(p -> p.startsWith("DB_PASSWORD contient un caractère invisible"));
    }

    @Test
    void motDePasseAdminTropCourt_refuse() {
        assertThat(ProductionConfigValidator.verifier(SECRET_SOLIDE, "Court1!", "Xk29!pqLm#v7"))
            .singleElement().asString().startsWith("ADMIN_DEFAULT_PASSWORD trop court");
    }

    @Test
    void secretJwtTropCourt_refuse() {
        assertThat(ProductionConfigValidator.verifier("court", MDP_SOLIDE, "Xk29!pqLm#v7"))
            .singleElement().asString().startsWith("JWT_SECRET trop court");
    }

    @Test
    void secretDuComposeDeDev_refuse() {
        assertThat(ProductionConfigValidator.verifier("change-me-in-production-please-use-a-long-random-secret", MDP_SOLIDE, "Xk29!pqLm#v7"))
            .singleElement().asString().startsWith("JWT_SECRET est une valeur de développement");
    }
}
