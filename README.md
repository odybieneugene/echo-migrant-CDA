# Echo Migrant — version Java (Spring Boot + Angular + PostgreSQL)

Reconstruction du projet **Echo Migrant** (journal numérique sur les questions migratoires, initialement en Laravel + React) avec une stack Java, pour le stage CDA à l'AFPA : **Spring Boot 3 / Java 21**, **Angular**, **PostgreSQL**.

La version Laravel/React d'origine reste disponible à la racine du dépôt (`../src`) à titre de référence — rien n'y a été modifié.

## Démarrage rapide (Docker)

Depuis ce dossier (`echo-migrant-java/`), avec Docker Desktop lancé :

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

**Backend** (nécessite Java 21, Maven, et un PostgreSQL local sur le port 5432 avec une base `echo_migrant`) :

```bash
cd backend
mvn spring-boot:run
```

**Frontend** (nécessite Node 20+) :

```bash
cd frontend
npm install
npx ng serve
```

## Tests

```bash
cd backend
mvn test
```

Les tests d'intégration tournent sur une base H2 en mémoire (voir `src/test/resources/application-test.yml`), donc aucune base PostgreSQL n'est nécessaire pour les lancer.

## Points de vigilance corrigés par rapport à la version Laravel

Pendant l'audit du projet Laravel d'origine avant la réécriture, plusieurs incohérences ont été identifiées et corrigées ici (voir aussi les commentaires en tête de `backend/src/main/resources/db/migration/V1__initial_schema.sql`) :

- un seul système de rôles (table `roles` + table de liaison), au lieu d'une colonne `role` redondante avec une vraie table `roles` jamais réellement branchée ;
- une seule table de liaison article ↔ tag SEO, au lieu de deux tables qui faisaient doublon ;
- nommage cohérent des tables (`medias`, `statuts`) ;
- abonnement directement rattaché à l'utilisateur (1:N), la relation N:N redondante `souscrire` ayant été supprimée ;
- ajout d'un mécanisme de création automatique du premier compte administrateur (identifié comme besoin dans le projet d'origine, mais jamais implémenté côté Laravel).
