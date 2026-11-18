-- Echo Migrant - schéma initial PostgreSQL (traduit et nettoyé depuis le MLD / migrations Laravel)
-- Corrections apportées par rapport à la version Laravel d'origine :
--   * un seul système de rôles (table roles + utilisateur_role), suppression de la colonne role redondante
--   * une seule table de liaison article <-> tag SEO (article_tag_seo), suppression du doublon "appartenir_a"
--   * nommage cohérent des tables (medias, statuts) au lieu du mélange singulier/pluriel de l'original
--   * abonnement rattaché directement à un utilisateur (1:N), suppression de la table pivot "souscrire" redondante

CREATE TABLE utilisateurs (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE utilisateur_role (
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (utilisateur_id, role_id)
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE type_articles (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE formats (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE statuts (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE auteurs (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100),
    email VARCHAR(150) NOT NULL UNIQUE,
    bio TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE tag_seos (
    id BIGSERIAL PRIMARY KEY,
    mot_cle VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE medias (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    url VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE abonnements (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('mensuel', 'annuel', 'don')),
    montant NUMERIC(8,2),
    date_debut DATE,
    date_fin DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'en_attente',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE dons (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    montant NUMERIC(8,2) NOT NULL,
    moyen_paiement VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    meta_titre VARCHAR(255),
    meta_description TEXT,
    meta_keywords VARCHAR(255),
    resume TEXT,
    contenu TEXT,
    image_couverture VARCHAR(500),
    categorie_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    type_article_id BIGINT REFERENCES type_articles(id) ON DELETE SET NULL,
    format_id BIGINT REFERENCES formats(id) ON DELETE SET NULL,
    statut_id BIGINT REFERENCES statuts(id) ON DELETE SET NULL,
    utilisateur_id BIGINT REFERENCES utilisateurs(id) ON DELETE SET NULL,
    est_en_vedette BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_articles_created_at ON articles (created_at);
CREATE INDEX idx_articles_est_en_vedette ON articles (est_en_vedette);
CREATE INDEX idx_articles_statut_created ON articles (statut_id, created_at);

CREATE TABLE commentaires (
    id BIGSERIAL PRIMARY KEY,
    contenu TEXT NOT NULL,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Article <-> Media (un article peut contenir plusieurs médias)
CREATE TABLE contenirs (
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    media_id BIGINT NOT NULL REFERENCES medias(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, media_id)
);

-- Utilisateur <-> Article (co-rédacteurs)
CREATE TABLE redigers (
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    PRIMARY KEY (utilisateur_id, article_id)
);

-- Article <-> TagSeo
CREATE TABLE article_tag_seo (
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    tag_seo_id BIGINT NOT NULL REFERENCES tag_seos(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, tag_seo_id)
);

-- Article <-> Abonnement (contrôle d'accès : quels abonnements donnent accès à quel article premium)
CREATE TABLE acceders (
    article_id BIGINT NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    abonnement_id BIGINT NOT NULL REFERENCES abonnements(id) ON DELETE CASCADE,
    PRIMARY KEY (article_id, abonnement_id)
);
