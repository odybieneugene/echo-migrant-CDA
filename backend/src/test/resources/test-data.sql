-- Rôles de référence pour les tests (Flyway est désactivé en test, voir application-test.yml).
-- Mêmes valeurs que la migration V2__reference_data_seed.sql.
INSERT INTO roles (nom, description) VALUES
    ('admin', 'Administrateur de la plateforme'),
    ('journaliste', 'Journaliste habilité à publier'),
    ('redacteur', 'Rédacteur / co-auteur d''articles'),
    ('lecteur', 'Utilisateur lecteur standard');

INSERT INTO statuts (nom, description) VALUES
    ('brouillon', 'Article en cours de rédaction, non visible publiquement'),
    ('publie', 'Article publié et visible publiquement'),
    ('archive', 'Article archivé, retiré de la mise en avant');
