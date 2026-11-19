-- Les articles enregistrés sans statut (statut_id NULL) deviennent des brouillons.
-- Ils étaient déjà traités comme tels (invisibles du public) mais s'affichaient « — » dans le dashboard.
-- Depuis cette version, ArticleService donne le statut « brouillon » à tout article enregistré sans statut.
UPDATE articles
SET statut_id = (SELECT id FROM statuts WHERE nom = 'brouillon'),
    updated_at = now()
WHERE statut_id IS NULL;
