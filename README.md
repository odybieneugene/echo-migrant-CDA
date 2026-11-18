# Echo Migrant — version Java (Spring Boot + Angular + PostgreSQL)

[![CI](https://github.com/odybieneugene/echo-migrant-CDA/actions/workflows/ci.yml/badge.svg)](https://github.com/odybieneugene/echo-migrant-CDA/actions/workflows/ci.yml)

Reconstruction du projet **Echo Migrant** (journal numérique sur les questions migratoires, initialement en Laravel + React) avec une stack Java, pour le stage CDA à l'AFPA : **Spring Boot 3 / Java 21**, **Angular**, **PostgreSQL**.

La version Laravel/React d'origine se trouve dans un dépôt séparé, conservé à titre de référence : [odybieneugene/echo-migrant](https://github.com/odybieneugene/echo-migrant).

## Démarrage rapide (Docker)

À la racine du dépôt, avec Docker Desktop lancé :

```bash
docker compose up --build
```

Cela démarre :

| Service    | URL                              |
|------------|-----------------------------------|
| Frontend   | http://localhost:4300             |
| Backend API| http://localhost:8081/api         |
| PostgreSQL | localhost:5433 (db: `echo_migrant`, user/pass: `echo_migrant`) |
| pgAdmin    | http://localhost:5051             |

Au premier démarrage, les migrations Flyway créent le schéma et les données de référence (rôles, statuts, formats, types d'article, catégories), et un compte administrateur est créé automatiquement s'il n'en existe aucun :

- Email : `admin@echomigrant.local`
- Mot de passe : `ChangeMoi123!` (⚠️ à changer immédiatement en conditions réelles)

## Démarrage sans Docker (développement)

**Base de données** : le plus simple est de ne lancer que le PostgreSQL du `docker-compose.yml` (port **5433**) :

```bash
docker compose up -d postgres
```

**Backend** (nécessite Java 21 ou plus récent et Maven) :

```bash
cd backend
DB_PORT=5433 mvn spring-boot:run        # Bash
# $env:DB_PORT='5433'; mvn spring-boot:run   (PowerShell)
```

Sans `DB_PORT`, le backend cherche un PostgreSQL local sur le port 5432 (base, utilisateur et mot de passe : `echo_migrant`).

**Frontend** (nécessite Node 20+) :

```bash
cd frontend
npm install
npx ng serve
```

## Mise en production

Stack de production prête (`docker-compose.prod.yml`) : Angular compilé servi par nginx, qui relaie `/api` et `/uploads`
au backend ; backend en profil `prod`, qui **refuse de démarrer** si un secret manque ou est faible ; base et API non exposées.

```bash
cp .env.example .env      # puis y mettre de vrais secrets (commandes de génération dans le fichier)
docker compose -f docker-compose.prod.yml up -d --build
```

Guide complet (serveur, HTTPS avec Caddy, sauvegardes et restauration, mises à jour, dépannage) :
**[DEPLOIEMENT.md](DEPLOIEMENT.md)**. L'intégration continue (GitHub Actions) teste le backend sur Java 21,
teste et compile le front, et construit les images Docker à chaque push.

## Tests

```bash
cd backend
mvn test
```

Les tests d'intégration tournent sur une base H2 en mémoire (voir `src/test/resources/application-test.yml`), donc aucune base PostgreSQL n'est nécessaire pour les lancer.

Ils couvrent l'authentification, la visibilité des articles (brouillons), les commentaires, les droits d'accès aux ressources (401 / 403), l'article « en vedette » et les images de couverture.

## Données de démonstration

Pour voir le site avec du vrai contenu, `scripts/demo/import-articles-laravel.sql` importe les 9 articles de la version Laravel d'origine (textes, catégories, tags SEO et images de couverture). Le script est rejouable sans créer de doublon ; le mode d'emploi est en tête du fichier.

Ce ne sont pas des migrations Flyway : ces données de démo ne doivent pas être déployées en production.

## Points de vigilance corrigés par rapport à la version Laravel

Pendant l'audit du projet Laravel d'origine avant la réécriture, plusieurs incohérences ont été identifiées et corrigées ici (voir aussi les commentaires en tête de `backend/src/main/resources/db/migration/V1__initial_schema.sql`) :

- un seul système de rôles (table `roles` + table de liaison), au lieu d'une colonne `role` redondante avec une vraie table `roles` jamais réellement branchée ;
- une seule table de liaison article ↔ tag SEO, au lieu de deux tables qui faisaient doublon ;
- nommage cohérent des tables (`medias`, `statuts`) ;
- abonnement directement rattaché à l'utilisateur (1:N), la relation N:N redondante `souscrire` ayant été supprimée ;
- ajout d'un mécanisme de création automatique du premier compte administrateur (identifié comme besoin dans le projet d'origine, mais jamais implémenté côté Laravel).

## Corrections de sécurité et de fiabilité

Un audit de la version Java a ensuite permis de corriger (chaque point est couvert par un test) :

- **inscription** : le rôle envoyé par le client était appliqué, ce qui permettait de s'inscrire directement comme administrateur ; l'inscription publique donne désormais toujours le rôle `lecteur` ;
- **brouillons** : un brouillon était lisible publiquement en devinant son identifiant ; il renvoie maintenant 404, sauf pour l'équipe éditoriale (prévisualisation) ;
- **commentaires** : la liste renvoyait toujours une erreur 500 (relation chargée hors transaction) et n'était pas accessible aux visiteurs ; elle est maintenant publique pour les articles visibles, et la liste complète est réservée à l'admin ;
- **codes d'erreur** : 401 pour un visiteur non connecté, 403 (avec un message JSON) pour un accès refusé, au lieu de 400 ou de réponses sans corps ;
- **règle de sécurité inutile** : suppression d'un accès public sur une route `POST /api/utilisateurs` inexistante ;
- **performances** : requêtes ciblées au lieu de charger des tables entières (`findAll()`) ;
- **images externes** : les images de couverture en URL (ex. Unsplash) s'affichent correctement ;
- **affichage des articles** : les paragraphes saisis dans le dashboard sont conservés.
