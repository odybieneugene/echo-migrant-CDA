-- Noms d'affichage des catégories créées par V2.
-- V2 utilisait des identifiants techniques (minuscules, tirets, sans accents) qui s'affichaient
-- tels quels dans les badges et les filtres du frontend. Le frontend s'appuie sur l'id, pas sur le nom :
-- le renommage ne casse aucun lien. Chaque UPDATE cible l'ancien nom : sans effet si la catégorie
-- a déjà été renommée à la main.

UPDATE categories SET nom = 'Immigration en Europe', updated_at = now() WHERE nom = 'immigration-europe';
UPDATE categories SET nom = 'Immigration en France', updated_at = now() WHERE nom = 'immigration-france';
UPDATE categories SET nom = 'Intégration',           updated_at = now() WHERE nom = 'integration';
UPDATE categories SET nom = 'Droit d''asile',        updated_at = now() WHERE nom = 'droit-asile';
