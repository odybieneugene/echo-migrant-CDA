# Echo Migrant — version CDA (Spring Boot + Angular + PostgreSQL)

Journal numérique sur les migrations. Réécriture Java du projet Laravel/React d'origine
(dossier parent `../`, dépôt git séparé — ne pas le modifier) pour le stage CDA à l'AFPA.
Dépôt GitHub : `odybieneugene/echo-migrant-CDA`. L'utilisateur parle français : répondre en français.

## Lancer le projet (Windows, sans Docker pour backend/frontend)

PostgreSQL tourne dans Docker (conteneur `echo_migrant_java_db`, port **5433**, pas 5432 —
le 5432 est pris par un autre projet, `mediatheque-db`).

```powershell
# Backend -> http://localhost:8081/api
cd backend
$env:DB_PORT='5433'; mvn spring-boot:run

# Frontend -> http://localhost:4300
cd frontend
npx ng serve --port 4300
```

Compte admin créé automatiquement au démarrage : `admin@echomigrant.local` / `ChangeMoi123!`.
Tout lancer en Docker : `docker compose up --build` depuis ce dossier.

Tests (H2 en mémoire, pas besoin de PostgreSQL) : `cd backend; mvn test`.

Données de démo : `scripts/demo/import-articles-laravel.sql` importe les 9 articles du projet
Laravel d'origine (images dans `scripts/demo/images/`). Rejouable sans doublon ; mode d'emploi en tête du fichier.
Ce n'est pas une migration Flyway (contenu de démo, pas pour la prod). La base MySQL d'origine
(conteneur `echo_migrant_db`, port 3307, root/root) est normalement arrêtée.

### Pièges connus de cet environnement
- **Seul JDK 26 est installé** (le projet cible Java 21). Le `pom.xml` contient donc :
  `lombok.version=1.18.48`, `maven.compiler.proc=full` (sinon Lombok ne tourne pas depuis JDK 23)
  et `argLine=-Dnet.bytebuddy.experimental=true` (Mockito). Ne pas les retirer.
- **PC à 8 Go de RAM, souvent saturé** : les serveurs lancés en arrière-plan par Claude Code
  peuvent être tués faute de mémoire. Limiter le heap si besoin :
  `mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Xmx384m"`.
- Si `ng` est introuvable ou que des types `@angular/*` manquent : `node_modules` est corrompu,
  le supprimer et faire `npm ci`.

## Architecture

### Backend (`backend/`, package `com.echomigrant`)
- `controller/` REST sous `/api/*` → `service/` (logique article/auth/fichiers) → `repository/` (Spring Data JPA) → `entity/`.
- `dto/` : records Java (`XxxRequest` en entrée, `XxxDto`/`XxxResponse` en sortie avec `from(entity)`).
- Schéma géré **uniquement par Flyway** (`src/main/resources/db/migration`, `ddl-auto: validate`).
  Toute modif de schéma = nouvelle migration `V3__...sql`, ne jamais éditer V1/V2.
- Sécurité : JWT stateless (`security/JwtUtil`, `JwtAuthenticationFilter`), règles d'URL dans
  `config/SecurityConfig`, droits fins via `@PreAuthorize` sur chaque méthode de contrôleur.
  Rôles : `admin`, `journaliste`, `redacteur`, `lecteur` (table `roles`, authorities `ROLE_XXX`).
- L'inscription publique (`/api/auth/register`) donne **toujours** le rôle `lecteur`
  (un test d'intégration le vérifie) — ne jamais laisser le client choisir son rôle.
  Seul l'admin change les rôles (`PUT /api/utilisateurs/{id}/roles`, onglet « Utilisateurs » du dashboard) ;
  `UtilisateurService` empêche de retirer son propre rôle admin, de supprimer son compte, et garde au moins un admin.
- `exception/GlobalExceptionHandler` : erreurs JSON `ApiError` ; accès refusé → 401 si anonyme, 403 si connecté.
  Pour refuser un accès dans le code (ex. « pas votre ressource »), lever `AccessDeniedException("message")` :
  jamais `BadRequestException` ni `ResponseEntity.status(403)` sans corps.
  Erreurs du client (JSON invalide, id non numérique, route inconnue…) → 4xx explicites ; le 500 générique
  loggue l'exception et ne renvoie **jamais** son message (pas de fuite de détails internes).
- `config/DataInitializer` crée l'admin par défaut s'il n'en existe aucun.
- Images d'articles : upload multipart, stockées dans `uploads/` (`service/FileStorageService`).

### Frontend (`frontend/`, Angular 18 standalone + Bootstrap)
- `src/app/pages/` : home, actualites, article-detail, login, dashboard (protégé par `roleGuard` : admin/journaliste/redacteur).
- `src/app/core/` : `services/` (appels API), `guards/`, `interceptors/auth.interceptor.ts` (ajoute le Bearer token), `models/models.ts`.
- Token + utilisateur stockés dans `localStorage` (`echo_migrant_token`, `echo_migrant_user`).
  Token expiré supprimé au démarrage (`AuthService.isExpired`) ; un 401 de l'API déconnecte et renvoie vers `/login?session=expiree`.
- `app.config.ts` enregistre la locale `fr-FR` (sans elle, les pipes `date` plantent : NG0701).
- URL de l'API : `src/environments/environment*.ts`.

## Règles métier et pièges du code
- `GET /api/articles/{id}` (public) : un brouillon ou un article sans statut renvoie 404, sauf pour
  admin/journaliste/redacteur (prévisualisation). Les articles `archive` restent lisibles par lien direct.
  Tout accès à un article par son id passe par `ArticleService.getVisibleOrThrow(id, utilisateur)`.
- `GET /api/commentaires?articleId=X` est public (si l'article est visible) ; sans `articleId` : admin uniquement.
  On ne peut commenter qu'un article visible.
- Un seul article « en vedette » à la fois (`ArticleService.retirerVedetteDesAutres`).
- `open-in-view: false` : une relation `LAZY` lue dans un DTO hors transaction plante
  (LazyInitializationException → 500). Charger la relation avec `@EntityGraph` ou construire le DTO dans un service `@Transactional`.
- Ne pas charger une table entière (`findAll()`) pour chercher une ligne : écrire une méthode de
  repository ciblée (`existsBy…`, `findBy…True`…). `findAll()` reste normal pour les routes « liste ».

## Tests (`backend/src/test`)
- Profil `test` : H2 mode PostgreSQL, Flyway désactivé, tables créées par Hibernate,
  rôles et statuts insérés par `src/test/resources/test-data.sql` (à compléter si un test a besoin d'autres données de référence).
- Tests d'intégration MockMvc (`@SpringBootTest` + `@ActiveProfiles("test")`) : un fichier par sujet
  (auth, visibilité des articles, commentaires, accès aux ressources, vedette). Le contexte et la base H2
  sont partagés entre classes : chaque test crée ses propres données (emails uniques) et n'asserte que sur elles.
- Compte admin disponible en test : `admin@echomigrant.local` / `ChangeMoi123!` (créé par `DataInitializer`).
- Toute correction de bug s'accompagne d'un test qui échouait avant la correction.
