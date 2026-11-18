-- =====================================================================================
-- Données de démonstration : les 9 articles du projet Laravel d'origine (base MySQL
-- echo_migrant), pour voir le rendu du nouveau frontend avec du vrai contenu.
--
-- PAS une migration Flyway : ce sont des données de contenu, à ne pas déployer en prod.
-- Le script est rejouable : il n'ajoute que ce qui manque (catégories, tags, articles
-- identifiés par leur titre) et ne modifie ni ne supprime les données existantes,
-- sauf la mise "en vedette" (un seul article à la fois : celui du projet d'origine).
--
-- Lancement (depuis echo-migrant-java/, conteneur PostgreSQL démarré ; marche dans PowerShell et Bash) :
--   1. copier les images :  cp scripts/demo/images/* backend/uploads/images/
--   2. envoyer le script :  docker cp scripts/demo/import-articles-laravel.sql echo_migrant_java_db:/tmp/import.sql
--   3. importer :           docker exec echo_migrant_java_db psql -U echo_migrant -d echo_migrant -v ON_ERROR_STOP=1 -f /tmp/import.sql
--
-- Correspondances avec le projet d'origine :
--   * auteur     : tous les articles sont attribués au premier administrateur (les comptes
--                  Laravel ne sont pas importés : leurs mots de passe ne doivent pas l'être)
--   * statut     : "publié" -> publie   |   format : "Articles texte" -> texte
--   * type       : le seul type d'origine ("texte") n'existe pas ici -> laissé vide
--   * images     : chemins images/xxx inchangés (fichiers dans scripts/demo/images),
--                  aucune image chargée depuis internet (démo possible hors ligne) :
--                  - "Crise migratoire" : photo Unsplash (licence Unsplash, usage libre) téléchargée
--                    dans images/unsplash-crise-migratoire.jpg
--                  - "Familles réfugiées" (photo d'un plongeur en vacances) et reportage Méditerranée
--                    (photo d'un logo Google) : images sans rapport retirées -> visuel par défaut du site
--   * corrections : "Voix en exil" est un podcast -> format podcast (il était en "texte" à l'origine)
-- =====================================================================================

BEGIN;

-- Catégories utilisées par les articles d'origine
INSERT INTO categories (nom) SELECT 'Actualités' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE nom = 'Actualités');
INSERT INTO categories (nom) SELECT 'Reportages' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE nom = 'Reportages');
INSERT INTO categories (nom) SELECT 'Société' WHERE NOT EXISTS (SELECT 1 FROM categories WHERE nom = 'Société');

-- Tags SEO liés aux articles
INSERT INTO tag_seos (mot_cle) SELECT 'migration' WHERE NOT EXISTS (SELECT 1 FROM tag_seos WHERE mot_cle = 'migration');
INSERT INTO tag_seos (mot_cle) SELECT 'réfugiés' WHERE NOT EXISTS (SELECT 1 FROM tag_seos WHERE mot_cle = 'réfugiés');
INSERT INTO tag_seos (mot_cle) SELECT 'intégration' WHERE NOT EXISTS (SELECT 1 FROM tag_seos WHERE mot_cle = 'intégration');
INSERT INTO tag_seos (mot_cle) SELECT 'témoignage' WHERE NOT EXISTS (SELECT 1 FROM tag_seos WHERE mot_cle = 'témoignage');

-- Un seul article en vedette : on retire la vedette actuelle si l'article vedette d'origine n'est pas encore importé
UPDATE articles SET est_en_vedette = FALSE WHERE est_en_vedette AND NOT EXISTS (SELECT 1 FROM articles WHERE titre = E'Crise migratoire : l\'Europe à la croisée des chemins 2025');

-- Articles
INSERT INTO articles (titre, meta_titre, meta_description, meta_keywords, resume, contenu, image_couverture, categorie_id, type_article_id, format_id, statut_id, utilisateur_id, est_en_vedette, created_at, updated_at)
SELECT E'Crise migratoire : l\'Europe à la croisée des chemins 2025', NULL, NULL, E'featuread,',
  E'Les pays européens cherchent un nouvel équilibre entre solidarité et souveraineté. Découvrez notre dossier spécial sur les politiques migratoires.',
  E'Alors que les tensions montent aux frontières, ce dossier explore les nouvelles approches migratoires adoptées par les gouvernements européens et leurs impacts humains.

Impossible de se défendre
Parmi les personnes interrogées, l\'ONG a relevé des cas de non-versement des salaires, de non-respect des horaires de travail, mais aussi de violences racistes répétées.

Ali, ressortissant indien, décrit un travail sous pression constante de ses supérieurs dans une chaîne multinationale, où il prépare des sandwichs. "La charge de travail était trop importante et nous devions travailler très vite. Parfois, ils nous criaient dessus pour que nous travaillions plus vite. [... Les responsables] nous criaient dessus, nous insultaient. Parfois, ils utilisaient le mot \'connard\', mais ils nous insultaient surtout parce que nous étions étrangers, [en raison de] notre couleur de peau. Ils nous disaient des choses comme \'Vous n\'êtes pas beaux\', \'Vous venez d\'un autre pays, donc vous n\'avez aucun droit\'", confie-t-il à Amnesty.

Titulaire d\'une carte de séjour d\'un an, Ali explique qu\'il lui était impossible de réagir ou de se défendre. "Ils nous menaçaient de nous licencier", dit-il. "Et nous, nous avions besoin de [cet emploi] pour renouveler nos papiers."

"Pour obtenir une carte de séjour, le demandeur ou la demandeuse doit disposer d\'une autorisation de travail, délivrée par la préfecture à la demande de l\'employeur·e. Mais pour obtenir une autorisation de travail, le demandeur ou la demandeuse doit disposer d\'une carte de séjour. Sans surprise, ce cercle vicieux entraîne des violations des droits des travailleuses et travailleurs étrangers. Nombre de travailleuses et travailleurs n\'ont d\'autre choix que de supporter des conditions de travail difficiles et dangereuses", souligne Amnesty.',
  'images/unsplash-crise-migratoire.jpg',
  (SELECT id FROM categories WHERE nom = 'Actualités'), NULL, (SELECT id FROM formats WHERE nom = 'texte'), (SELECT id FROM statuts WHERE nom = 'publie'),
  (SELECT ur.utilisateur_id FROM utilisateur_role ur JOIN roles r ON r.id = ur.role_id WHERE r.nom = 'admin' ORDER BY ur.utilisateur_id LIMIT 1),
  TRUE, '2025-10-21 09:09:05', '2025-11-13 10:01:40'
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE titre = E'Crise migratoire : l\'Europe à la croisée des chemins 2025');

INSERT INTO articles (titre, meta_titre, meta_description, meta_keywords, resume, contenu, image_couverture, categorie_id, type_article_id, format_id, statut_id, utilisateur_id, est_en_vedette, created_at, updated_at)
SELECT E'Familles réfugiées : parcours et défis humains', NULL, NULL, NULL,
  E'Un reportage exclusif sur les parcours de familles déplacées à travers l’Europe.',
  E'Entre frontières fermées et solidarité citoyenne, ce reportage met en lumière les défis humains derrière les chiffres.',
  NULL,
  (SELECT id FROM categories WHERE nom = 'Reportages'), NULL, (SELECT id FROM formats WHERE nom = 'texte'), (SELECT id FROM statuts WHERE nom = 'publie'),
  (SELECT ur.utilisateur_id FROM utilisateur_role ur JOIN roles r ON r.id = ur.role_id WHERE r.nom = 'admin' ORDER BY ur.utilisateur_id LIMIT 1),
  FALSE, '2025-10-21 09:09:05', '2025-11-13 08:42:08'
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE titre = E'Familles réfugiées : parcours et défis humains');

INSERT INTO articles (titre, meta_titre, meta_description, meta_keywords, resume, contenu, image_couverture, categorie_id, type_article_id, format_id, statut_id, utilisateur_id, est_en_vedette, created_at, updated_at)
SELECT E'Reportage : sur les routes des réfugiés en Méditerranée', NULL, NULL, NULL,
  E'Nos journalistes ont suivi les équipes humanitaires au large de la Sicile.',
  E'Plongée au cœur d’une mission de sauvetage, entre drames et espoirs sur les routes migratoires.',
  NULL,
  (SELECT id FROM categories WHERE nom = 'Reportages'), NULL, (SELECT id FROM formats WHERE nom = 'texte'), (SELECT id FROM statuts WHERE nom = 'publie'),
  (SELECT ur.utilisateur_id FROM utilisateur_role ur JOIN roles r ON r.id = ur.role_id WHERE r.nom = 'admin' ORDER BY ur.utilisateur_id LIMIT 1),
  FALSE, '2025-10-21 09:09:05', '2025-11-13 09:09:46'
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE titre = E'Reportage : sur les routes des réfugiés en Méditerranée');

INSERT INTO articles (titre, meta_titre, meta_description, meta_keywords, resume, contenu, image_couverture, categorie_id, type_article_id, format_id, statut_id, utilisateur_id, est_en_vedette, created_at, updated_at)
SELECT E'Les nouveaux défis de l’immigration en Europe en 2025', NULL, NULL, NULL,
  E'Alors que les flux migratoires vers l’Europe reprennent à la hausse, plusieurs pays membres de l’Union européenne cherchent à réformer leurs politiques d’accueil et d’intégration',
  E'En 2025, la question migratoire reste au cœur du débat européen. Selon les derniers chiffres publiés par Eurostat (octobre 2025), plus de 1,2 million de demandes d’asile ont été enregistrées depuis le début de l’année, soit une augmentation de 18 % par rapport à 2024.
Les principaux pays d’accueil demeurent l’Allemagne, la France, l’Espagne et l’Italie, tandis que des nations comme la Pologne et la Croatie connaissent une hausse notable des arrivées.

Face à ces évolutions, l’Union européenne travaille à renforcer le Pacte sur la migration et l’asile, dont l’application complète est prévue pour 2026. Ce texte vise à mieux répartir les responsabilités entre États membres, tout en améliorant les conditions d’accueil et les procédures de retour pour les personnes déboutées.

Plusieurs ONG, dont Médecins Sans Frontières et Amnesty International, appellent toutefois à davantage de garanties pour le respect des droits fondamentaux aux frontières, notamment en Méditerranée et dans les Balkans.

Malgré les tensions politiques, certains pays européens expérimentent de nouveaux modèles d’intégration, fondés sur l’accès rapide à l’emploi et la reconnaissance des diplômes étrangers.

Citation :

“L’immigration n’est pas seulement une question de contrôle des frontières, c’est une question de société et d’avenir partagé”, souligne Sophie Bernard, sociologue spécialiste des migrations à l’Université de Lyon.',
  'images/WNuvHrl2kF5wI0IUYJGd2y56GsMATiIMQQ1c3FRk.jpg',
  (SELECT id FROM categories WHERE nom = 'Actualités'), NULL, (SELECT id FROM formats WHERE nom = 'texte'), (SELECT id FROM statuts WHERE nom = 'publie'),
  (SELECT ur.utilisateur_id FROM utilisateur_role ur JOIN roles r ON r.id = ur.role_id WHERE r.nom = 'admin' ORDER BY ur.utilisateur_id LIMIT 1),
  FALSE, '2025-11-05 15:50:35', '2025-11-05 15:50:35'
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE titre = E'Les nouveaux défis de l’immigration en Europe en 2025');

INSERT INTO articles (titre, meta_titre, meta_description, meta_keywords, resume, contenu, image_couverture, categorie_id, type_article_id, format_id, statut_id, utilisateur_id, est_en_vedette, created_at, updated_at)
SELECT E'La jeunesse migrante, moteur invisible des économies européennes', NULL, NULL, E'jeunesse, migration, économie, emploi, intégration',
  E'Souvent perçus comme une charge, les jeunes migrants participent pourtant activement à la croissance économique et au renouvellement démographique en Europe',
  E'En 2025, près d’un quart des nouveaux arrivants en Europe ont moins de 30 ans, selon les données du Haut-Commissariat des Nations Unies pour les réfugiés (HCR). Ces jeunes, venus principalement d’Afrique du Nord, du Moyen-Orient et d’Amérique latine, s’intègrent progressivement aux systèmes éducatifs et aux marchés du travail européens.

Dans plusieurs pays, comme la France, l’Espagne et l’Allemagne, ils occupent des secteurs en tension — hôtellerie, bâtiment, agriculture, soins à la personne — contribuant à compenser la baisse de main-d’œuvre locale.
“Sans eux, certains services essentiels cesseraient tout simplement de fonctionner”, souligne Lucia Martínez, économiste au Centre européen des migrations.

Malgré ces apports, les obstacles à l’intégration restent nombreux : précarité administrative, difficultés linguistiques et reconnaissance incomplète des diplômes. De nouvelles initiatives voient cependant le jour : formations linguistiques accélérées, programmes de mentorat, et partenariats entre universités et associations locales.

À Berlin, le projet “Next Step” accompagne chaque année plus de 500 jeunes réfugiés vers une formation qualifiante. En France, plusieurs municipalités expérimentent des parcours d’intégration territoriale fondés sur la participation citoyenne et la culture.

“Ces jeunes ne fuient pas seulement quelque chose, ils viennent aussi construire quelque chose ici”, déclare Youssef Benali, coordinateur d’un centre d’accueil à Lyon.',
  NULL,
  (SELECT id FROM categories WHERE nom = 'Actualités'), NULL, (SELECT id FROM formats WHERE nom = 'texte'), (SELECT id FROM statuts WHERE nom = 'publie'),
  (SELECT ur.utilisateur_id FROM utilisateur_role ur JOIN roles r ON r.id = ur.role_id WHERE r.nom = 'admin' ORDER BY ur.utilisateur_id LIMIT 1),
  FALSE, '2025-11-06 12:53:17', '2025-11-13 09:08:08'
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE titre = E'La jeunesse migrante, moteur invisible des économies européennes');

INSERT INTO articles (titre, meta_titre, meta_description, meta_keywords, resume, contenu, image_couverture, categorie_id, type_article_id, format_id, statut_id, utilisateur_id, est_en_vedette, created_at, updated_at)
SELECT E'"Les femmes migrantes, entre courage et invisibilité"', NULL, NULL, NULL,
  E'Souvent en première ligne des parcours migratoires, les femmes représentent près de la moitié des personnes en mouvement à travers le monde, mais leurs voix restent encore trop peu entendues.',
  E'Selon l’Organisation internationale pour les migrations (OIM), les femmes constituent 48 % des migrants internationaux en 2025. Elles quittent leurs pays pour des raisons diverses : conflits, violences, pauvreté, mais aussi pour rejoindre des proches ou chercher une vie meilleure.

Pourtant, leur expérience diffère profondément de celle des hommes. En chemin, beaucoup subissent des violences sexuelles, des discriminations ou des exploitations économiques. Une étude de l’Université d’Oxford (avril 2025) révèle que plus de 65 % des femmes migrantes ont connu au moins un épisode de violence physique ou psychologique pendant leur parcours.

En Europe, des associations comme France Terre d’Asile, La Cimade ou Women Refugee Route militent pour une meilleure prise en charge des femmes migrantes, notamment en matière de santé reproductive, d’hébergement sécurisé et d’accès à l’emploi.

À Marseille, un centre d’accueil pilote propose des ateliers d’autonomisation où les participantes apprennent la langue, découvrent leurs droits et développent des projets professionnels. “C’est un espace pour reprendre confiance en soi”, témoigne Aïssata, réfugiée malienne installée depuis deux ans.

Malgré les progrès réalisés, le manque de visibilité médiatique reste un défi majeur. Les chercheuses appellent à intégrer davantage la voix des femmes dans la construction des politiques migratoires.',
  'images/mK5p5ubdXZ4fsmV5fYnfQ34xa5olgR2iF12fh8H0.jpg',
  (SELECT id FROM categories WHERE nom = 'Actualités'), NULL, (SELECT id FROM formats WHERE nom = 'texte'), (SELECT id FROM statuts WHERE nom = 'publie'),
  (SELECT ur.utilisateur_id FROM utilisateur_role ur JOIN roles r ON r.id = ur.role_id WHERE r.nom = 'admin' ORDER BY ur.utilisateur_id LIMIT 1),
  FALSE, '2025-11-06 13:03:31', '2025-11-13 09:01:31'
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE titre = E'"Les femmes migrantes, entre courage et invisibilité"');

INSERT INTO articles (titre, meta_titre, meta_description, meta_keywords, resume, contenu, image_couverture, categorie_id, type_article_id, format_id, statut_id, utilisateur_id, est_en_vedette, created_at, updated_at)
SELECT E'« Les nouveaux visages de la solidarité : quand les jeunes s’engagent pour les migrants »', NULL, NULL, NULL,
  E'En France comme ailleurs, une nouvelle génération s’investit dans l’accueil et l’intégration des personnes migrantes. Entre engagement citoyen, initiatives locales et innovation sociale, ces jeunes changent le regard sur la migration.',
  E'À Marseille, Paris, Lille ou encore Brive-la-Gaillarde, des associations étudiantes multiplient les projets solidaires : cours de français, ateliers d’insertion numérique, collectes alimentaires, mais aussi accompagnement juridique.
Selon le ministère de la Jeunesse, le nombre de bénévoles de moins de 30 ans impliqués dans la cause migratoire a augmenté de 22 % depuis 2022.
Pour beaucoup, il ne s’agit pas seulement d’aider, mais de créer du lien. « C’est une manière de bâtir une société plus juste et plus humaine », confie Sarah, 25 ans, bénévole à “Jeunes Solidaires”.',
  NULL,
  (SELECT id FROM categories WHERE nom = 'Actualités'), NULL, (SELECT id FROM formats WHERE nom = 'texte'), (SELECT id FROM statuts WHERE nom = 'publie'),
  (SELECT ur.utilisateur_id FROM utilisateur_role ur JOIN roles r ON r.id = ur.role_id WHERE r.nom = 'admin' ORDER BY ur.utilisateur_id LIMIT 1),
  FALSE, '2025-11-13 08:33:55', '2025-11-13 08:56:18'
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE titre = E'« Les nouveaux visages de la solidarité : quand les jeunes s’engagent pour les migrants »');

INSERT INTO articles (titre, meta_titre, meta_description, meta_keywords, resume, contenu, image_couverture, categorie_id, type_article_id, format_id, statut_id, utilisateur_id, est_en_vedette, created_at, updated_at)
SELECT E'« Voix en exil : le podcast qui redonne la parole aux oubliés »', NULL, NULL, NULL,
  E'Chaque semaine, le podcast Voix en exil donne la parole à des migrants venus raconter leur parcours, leurs espoirs et leurs rêves. Une production signée Echo Migrant.',
  E'Lancé en octobre 2025, Voix en exil s’impose comme une chronique humaine au cœur de l’actualité.
Chaque épisode de 20 minutes met en lumière un témoignage, souvent bouleversant, d’hommes et de femmes qui ont traversé des frontières et des épreuves.
Derrière ce projet : une volonté simple, celle de rendre visibles celles et ceux qu’on entend trop peu.
Disponible sur Spotify, Deezer et Apple Podcasts, Voix en exil connaît déjà un succès croissant auprès d’un public jeune et engagé.',
  'images/ajlaQn1nz0i09DpLOcCfho4fb5w57aWM6xibqjtn.webp',
  (SELECT id FROM categories WHERE nom = 'Actualités'), NULL, (SELECT id FROM formats WHERE nom = 'podcast'), (SELECT id FROM statuts WHERE nom = 'publie'),
  (SELECT ur.utilisateur_id FROM utilisateur_role ur JOIN roles r ON r.id = ur.role_id WHERE r.nom = 'admin' ORDER BY ur.utilisateur_id LIMIT 1),
  FALSE, '2025-11-13 09:26:23', '2025-11-13 09:26:23'
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE titre = E'« Voix en exil : le podcast qui redonne la parole aux oubliés »');

INSERT INTO articles (titre, meta_titre, meta_description, meta_keywords, resume, contenu, image_couverture, categorie_id, type_article_id, format_id, statut_id, utilisateur_id, est_en_vedette, created_at, updated_at)
SELECT E'« Le numérique au service de l’intégration : quand la tech facilite l’accueil des migrants »', NULL, NULL, NULL,
  E'Applications mobiles, plateformes d’apprentissage en ligne, intelligence artificielle traductive… les outils numériques transforment aujourd’hui l’intégration des personnes migrantes en Europe.',
  E'À Lyon, une start-up sociale a développé une application baptisée LinguaConnect, qui permet aux nouveaux arrivants d’apprendre les bases du français à travers des modules audio-visuels et des exercices pratiques.
À Berlin, un collectif d’ingénieurs bénévoles travaille sur une carte interactive répertoriant les associations d’aide aux migrants selon leur localisation et leurs besoins (hébergement, santé, formation, emploi).
Ces initiatives démontrent qu’au-delà des débats politiques, la technologie peut être un levier concret d’inclusion.
Pour Leïla Benali, coordinatrice de projet chez Tech4Solidarity, « le numérique ne remplace pas la solidarité humaine, mais il l’amplifie ».',
  'images/TmVd3sJmSKlrQpiApct5cAlNTRDcyU0bEQh0USbZ.png',
  (SELECT id FROM categories WHERE nom = 'Société'), NULL, (SELECT id FROM formats WHERE nom = 'texte'), (SELECT id FROM statuts WHERE nom = 'publie'),
  (SELECT ur.utilisateur_id FROM utilisateur_role ur JOIN roles r ON r.id = ur.role_id WHERE r.nom = 'admin' ORDER BY ur.utilisateur_id LIMIT 1),
  FALSE, '2025-11-13 09:28:58', '2025-11-13 10:01:40'
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE titre = E'« Le numérique au service de l’intégration : quand la tech facilite l’accueil des migrants »');

-- Liens article <-> tag SEO
INSERT INTO article_tag_seo (article_id, tag_seo_id) SELECT a.id, t.id FROM articles a, tag_seos t WHERE a.titre = E'Crise migratoire : l\'Europe à la croisée des chemins 2025' AND t.mot_cle = 'migration' ON CONFLICT DO NOTHING;
INSERT INTO article_tag_seo (article_id, tag_seo_id) SELECT a.id, t.id FROM articles a, tag_seos t WHERE a.titre = E'"Les femmes migrantes, entre courage et invisibilité"' AND t.mot_cle = 'réfugiés' ON CONFLICT DO NOTHING;
INSERT INTO article_tag_seo (article_id, tag_seo_id) SELECT a.id, t.id FROM articles a, tag_seos t WHERE a.titre = E'« Voix en exil : le podcast qui redonne la parole aux oubliés »' AND t.mot_cle = 'intégration' ON CONFLICT DO NOTHING;
INSERT INTO article_tag_seo (article_id, tag_seo_id) SELECT a.id, t.id FROM articles a, tag_seos t WHERE a.titre = E'« Le numérique au service de l’intégration : quand la tech facilite l’accueil des migrants »' AND t.mot_cle = 'intégration' ON CONFLICT DO NOTHING;
INSERT INTO article_tag_seo (article_id, tag_seo_id) SELECT a.id, t.id FROM articles a, tag_seos t WHERE a.titre = E'« Le numérique au service de l’intégration : quand la tech facilite l’accueil des migrants »' AND t.mot_cle = 'témoignage' ON CONFLICT DO NOTHING;

-- Corrections appliquées aussi à une base importée avant elles (sans effet si déjà faites)
UPDATE articles SET format_id = (SELECT id FROM formats WHERE nom = 'podcast'), updated_at = now()
WHERE titre = E'« Voix en exil : le podcast qui redonne la parole aux oubliés »'
  AND format_id IS DISTINCT FROM (SELECT id FROM formats WHERE nom = 'podcast');
UPDATE articles SET image_couverture = NULL, updated_at = now()
WHERE image_couverture LIKE 'https://images.unsplash.com/photo-1529612700005-%'
   OR image_couverture LIKE 'https://images.unsplash.com/photo-1534081333815-%';
UPDATE articles SET image_couverture = 'images/unsplash-crise-migratoire.jpg', updated_at = now()
WHERE image_couverture LIKE 'https://images.unsplash.com/photo-1509099836639-%';

COMMIT;
