-- Catégories liées au menu principal (maquette : « Actualités », « Reportages », « Enquêtes »).
-- Le menu du frontend les retrouve par leur NOM : elles doivent exister sur toute base, y compris en production
-- (jusqu'ici « Actualités » et « Reportages » n'étaient créées que par le script de démo).
-- Chaque INSERT est sans effet si la catégorie existe déjà (base de démo, ou création à la main).
INSERT INTO categories (nom, description)
SELECT 'Actualités', 'L''actualité des migrations'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE nom = 'Actualités');

INSERT INTO categories (nom, description)
SELECT 'Reportages', 'Reportages vidéo, photo et audio sur le terrain'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE nom = 'Reportages');

INSERT INTO categories (nom, description)
SELECT 'Enquêtes', 'Enquêtes et dossiers de fond'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE nom = 'Enquêtes');
