-- Recherche paginée côté serveur (GET /api/articles/recherche).
-- texte_recherche = titre + résumé en minuscules et sans accents : chercher « societe » trouve « Société ».
-- La colonne est tenue à jour par l'entité Article (@PrePersist / @PreUpdate, même normalisation qu'ici).
ALTER TABLE articles ADD COLUMN texte_recherche TEXT;

-- Articles existants : lower() puis retrait des accents du français (sans l'extension unaccent).
UPDATE articles
SET texte_recherche = translate(
        lower(titre || ' ' || coalesce(resume, '')),
        'àáâãäåçèéêëìíîïñòóôõöùúûüýÿ',
        'aaaaaaceeeeiiiinooooouuuuyy');

-- NOT NULL + défaut vide : une insertion SQL directe (script de démo) reste possible ; elle doit ensuite
-- recalculer la colonne (voir scripts/demo/import-articles-laravel.sql), sinon l'article est introuvable par la recherche.
ALTER TABLE articles ALTER COLUMN texte_recherche SET DEFAULT '';
ALTER TABLE articles ALTER COLUMN texte_recherche SET NOT NULL;
