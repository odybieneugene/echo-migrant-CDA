-- Inscriptions à la newsletter (formulaire public de la page d'accueil).
-- Distinct de "abonnements" (formules payantes liées à un compte) : ici, un simple email, sans compte.
-- L'email est stocké en minuscules par l'application ; UNIQUE empêche les doublons même en cas de requêtes simultanées.
CREATE TABLE newsletter_inscriptions (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
