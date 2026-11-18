-- Données de référence de base (équivalent des seeders Laravel)

INSERT INTO roles (nom, description) VALUES
    ('admin', 'Administrateur de la plateforme'),
    ('journaliste', 'Journaliste habilité à publier'),
    ('redacteur', 'Rédacteur / co-auteur d''articles'),
    ('lecteur', 'Utilisateur lecteur standard');

INSERT INTO statuts (nom, description) VALUES
    ('brouillon', 'Article en cours de rédaction, non visible publiquement'),
    ('publie', 'Article publié et visible publiquement'),
    ('archive', 'Article archivé, retiré de la mise en avant');

INSERT INTO formats (nom, description) VALUES
    ('texte', 'Article texte classique'),
    ('video', 'Contenu vidéo'),
    ('podcast', 'Contenu audio / podcast');

INSERT INTO type_articles (nom, description) VALUES
    ('actualite', 'Article d''actualité'),
    ('reportage', 'Reportage de terrain'),
    ('enquete', 'Enquête / investigation'),
    ('temoignage', 'Témoignage personnel');

INSERT INTO categories (nom, description) VALUES
    ('immigration-europe', 'Immigration en Europe'),
    ('immigration-france', 'Immigration en France'),
    ('integration', 'Intégration et vie quotidienne'),
    ('droit-asile', 'Droit d''asile et procédures');
