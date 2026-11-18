# Soutenance — Echo Migrant (CDA)

Aide-mémoire pour la présentation au jury : préparation du poste, comptes de démonstration,
scénario pas à pas, points techniques à mettre en avant, évolutions prévues, questions probables.

---

## 1. Le projet en une minute

**Echo Migrant** est un journal numérique sur les questions migratoires : articles, reportages, podcasts
et vidéos, commentaires des lecteurs, newsletter, et un espace de rédaction avec des rôles.

C'est la **réécriture** d'un premier projet Laravel/React en **Spring Boot 3.4 (Java 21) + Angular 18 + PostgreSQL 16**.

| Bloc CDA | Ce qui le montre dans le projet |
|---|---|
| Développer une application sécurisée | Front Angular (composants standalone, guards, intercepteur), API REST Spring, sécurité JWT + rôles |
| Concevoir et développer une application organisée en couches | contrôleur → service → repository → entité, DTO, base relationnelle (20 tables), migrations Flyway |
| Préparer le déploiement d'une application sécurisée | 62 tests backend + 7 tests front, CI GitHub Actions, Docker de production, guide `DEPLOIEMENT.md` |

**C'est une application web responsive.** Elle est utilisable sur téléphone (contrôlée à 390 px). Une
application mobile native est une évolution possible : l'API REST est déjà prête pour la servir (§ 6).

## 2. Avant la soutenance (30 min avant)

Le PC a 8 Go de RAM : **fermer IntelliJ (≈ 1 Go), les onglets Chrome inutiles, Teams, etc.**

```powershell
# 1. Docker Desktop lancé, puis la base (conteneur echo_migrant_java_db, port 5433)
docker start echo_migrant_java_db

# 2. Backend -> http://localhost:8081/api   (attendre "Started EchoMigrantApplication")
cd backend
$env:DB_PORT='5433'; mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Xmx384m"

# 3. Frontend -> http://localhost:4300        (autre terminal)
cd frontend
npx ng serve --port 4300

# 4. Comptes de démo (rejouable sans risque)
python scripts/demo/creer-comptes-demo.py
```

Vérifications rapides :
- [ ] http://localhost:8081/api/ping répond « fonctionne »
- [ ] l'accueil s'affiche avec ses images, l'article en vedette en haut
- [ ] connexion possible avec chacun des 4 comptes ci-dessous
- [ ] onglets prêts : le site, GitHub (onglet **Actions**, CI verte), VS Code sur le projet
- [ ] un **deuxième navigateur** (ou une fenêtre privée) pour être connecté sous deux rôles à la fois

**Plan B** si la démo en direct plante : les captures du dossier de présentation, et le dépôt GitHub
(code, CI verte, `DEPLOIEMENT.md`). Ne pas déboguer devant le jury : passer au plan B et l'expliquer.

## 3. Comptes de démonstration

| Rôle | Email | Mot de passe | Peut |
|---|---|---|---|
| Administrateur | `admin@echomigrant.local` | `ChangeMoi123!` | tout, plus gérer les utilisateurs et la newsletter |
| Journaliste | `journaliste@echomigrant.local` | `Demo2026!` | écrire, modifier et supprimer **tous** les articles |
| Rédacteur | `redacteur@echomigrant.local` | `Demo2026!` | écrire, modifier **ses** articles ; ne supprime pas |
| Lecteur | `lecteur@echomigrant.local` | `Demo2026!` | lire, commenter (rôle donné à toute inscription) |

Comptes de **développement** uniquement : en production le backend refuse ces mots de passe (§ 5).

Matrice des droits, vérifiée sur l'API le 18/11/2026 (codes HTTP) :

| Action | Anonyme | Lecteur | Rédacteur | Journaliste | Admin |
|---|---|---|---|---|---|
| Lire les articles publiés | 200 | 200 | 200 | 200 | 200 |
| Lire un brouillon | 404 | 404 | 200 | 200 | 200 |
| Dashboard (liste de rédaction) | 401 | 403 | 200 | 200 | 200 |
| Modifier l'article d'un rédacteur | 401 | 403 | 200 (le sien) / 403 (un autre) | 200 | 200 |
| Supprimer un article | 401 | 403 | 403 | 204 | 204 |
| Utilisateurs, inscrits newsletter | 401 | 403 | 403 | 403 | 200 |

## 4. Scénario de démonstration (≈ 15 min)

**1. Visiteur (2 min)** — non connecté.
- Accueil : article en vedette, derniers articles, podcasts et vidéos, newsletter.
- *Actualités* : filtre par catégorie. Ouvrir un article.
- S'inscrire à la newsletter. Recommencer avec le même email : même message, sans doublon
  (on ne révèle pas si un email est déjà inscrit).
- Réduire la fenêtre (ou ouvrir les outils de développement en mode téléphone) : menu hamburger, mise en page adaptée.

**2. Lecteur (2 min)** — `lecteur@…`
- Commenter un article. Montrer que **Dashboard** n'apparaît pas dans le menu.
- Taper `/dashboard` dans l'adresse : renvoyé vers l'accueil (guard Angular `roleGuard`).
  *Et surtout* : même en appelant l'API directement, le serveur répond 403. Le front ne fait que masquer ; c'est le backend qui décide.
- *Mon compte* : changer son mot de passe (l'ancien est demandé).

**3. Rédacteur (3 min)** — `redacteur@…`
- Dashboard : il ne voit **Modifier** que sur **son** brouillon, et aucun bouton **Supprimer**.
- Créer un article avec image, en brouillon : il n'apparaît pas sur le site public.
- Point à raconter : *en préparant cette démo, j'ai découvert qu'un rédacteur pouvait modifier l'article
  d'un collègue. J'ai écrit un test qui reproduisait le problème, corrigé le service, et le test passe
  maintenant.* C'est la démarche « test d'abord » (`DroitsRedactionIntegrationTest`).

**4. Journaliste (2 min)** — `journaliste@…`
- Relire le brouillon du rédacteur, le **publier**, le mettre **en vedette** : il remplace l'ancien
  article à la une (un seul article en vedette à la fois).
- Retour sur l'accueil : le nouvel article est en haut.

**5. Administrateur (3 min)** — `admin@…`
- Onglet **Utilisateurs** : passer un lecteur en rédacteur. Il est impossible de retirer son propre rôle
  admin ou de supprimer son propre compte, et il reste toujours au moins un admin.
- Onglet **Newsletter** : liste des inscrits, suppression (droit à l'effacement, RGPD).

**6. Qualité et déploiement (3 min)** — dans VS Code et GitHub
- Structure du backend (`controller` → `service` → `repository` → `entity`, DTO), une migration Flyway.
- Lancer les tests ou montrer l'onglet **Actions** de GitHub : 3 jobs verts (backend Java 21,
  front, images Docker).
- `docker-compose.prod.yml` + `DEPLOIEMENT.md` : nginx devant, base et API non exposées, HTTPS avec
  Caddy, secrets dans `.env`, sauvegardes testées.

## 5. Points techniques à mettre en avant

**Sécurité**
- Mots de passe hachés avec **BCrypt**, jamais stockés en clair.
- **JWT** signé (HMAC-SHA), valable 24 h, envoyé dans l'en-tête `Authorization` par un intercepteur Angular.
  Un jeton expiré est supprimé au démarrage ; un 401 déconnecte et affiche « session expirée ».
- Droits vérifiés **côté serveur** sur chaque route (`@PreAuthorize`) et dans le service quand ça
  dépend de la donnée (auteur de l'article, brouillon visible ou non). Le front ne fait que masquer.
- L'inscription donne **toujours** le rôle lecteur : le client ne peut pas choisir son rôle (un test le vérifie).
- Requêtes SQL via JPA, donc paramétrées (pas d'injection SQL). Données d'entrée validées (`@Valid`).
- Erreurs : JSON clair en 4xx. Une erreur 500 est journalisée mais **ne renvoie jamais de détail interne**.
- Profil production : l'application **refuse de démarrer** si un secret manque, est trop court, est une
  valeur de développement connue ou contient un caractère invisible.
- Images : extension autorisée et taille contrôlée (4 Mo), fichier renommé avec un UUID (pas de nom
  choisi par l'utilisateur, donc pas de chemin piégé). Amélioration prévue : vérifier le contenu réel du fichier.

**Organisation du code**
- DTO en `record` Java : l'API n'expose jamais une entité JPA (ni mot de passe, ni relation non chargée).
- Schéma géré **uniquement** par Flyway (`ddl-auto: validate`) : la base de production évolue par migrations versionnées.
- `open-in-view: false` : pas de requête cachée hors transaction.

**Tests et qualité**
- 62 tests backend (intégration MockMvc sur une base H2, plus des tests unitaires), 7 tests front.
- La règle du projet : toute correction de bug est accompagnée d'un test qui échouait avant.
- CI GitHub Actions à chaque push.

**Déploiement**
- Images Docker multi-étapes : on compile dans une image, on exécute dans une image légère, avec un utilisateur non root.
- nginx sert Angular et relaie `/api`. Un seul domaine, donc pas de CORS.
- Piège trouvé et documenté : un port publié par Docker **contourne le pare-feu UFW**, d'où l'écoute en `127.0.0.1` derrière Caddy.

## 6. Évolutions prévues

| Évolution | État actuel |
|---|---|
| **Abonnements payants et dons** | tables et routes API présentes (`/api/abonnements`, `/api/dons`) ; il manquent l'interface et un vrai paiement (Stripe, en mode test d'abord) |
| **Envoi réel de la newsletter** | inscriptions stockées ; à brancher sur un service d'emailing, avec un lien de désinscription |
| **Application mobile** | l'API REST + JWT est réutilisable telle quelle (Ionic/Capacitor à partir du code Angular, ou Flutter) ; possible aussi : PWA installable |
| **Déploiement continu (CD)** | la CI construit déjà les images ; étape suivante, les publier sur GitHub Container Registry et déployer automatiquement |
| **Sécurité** | limitation des tentatives de connexion (anti force brute), jeton de rafraîchissement, cookie `HttpOnly` au lieu du `localStorage` |
| **Statut des articles** | un circuit de relecture (« à relire » avant publication) et un historique des modifications |
| Divers | contrôle du contenu réel des images envoyées, pagination côté serveur, recherche plein texte, tests de bout en bout (Playwright), accessibilité RGAA vérifiée |

## 7. Questions probables du jury

**Pourquoi un JWT plutôt qu'une session ?**
L'API est sans état (stateless) : aucun stockage de session côté serveur, donc elle passe facilement à
plusieurs instances, et le même jeton servirait à une application mobile. La contrepartie : un jeton ne
peut pas être révoqué avant son expiration. C'est pour ça que la durée est limitée (24 h) ; un jeton de
rafraîchissement est prévu.

**Le jeton dans le `localStorage`, ce n'est pas risqué ?**
Si. Il est lisible par du JavaScript en cas de faille XSS. Angular échappe par défaut tout ce qu'il
affiche, ce qui limite ce risque. La solution plus robuste est un cookie `HttpOnly` + `SameSite`
(dans les évolutions), qui demanderait alors une protection CSRF.

**Pourquoi la protection CSRF est désactivée ?**
Le jeton est envoyé dans un en-tête `Authorization` ajouté par le code, pas dans un cookie : un site
tiers ne peut pas le faire envoyer automatiquement par le navigateur. Le CSRF vise les cookies.

**Si on cache le bouton dans Angular, est-ce sécurisé ?**
Non, et ce n'est pas le rôle du front. Le masquage sert l'ergonomie ; la sécurité est dans l'API
(`@PreAuthorize` + vérification de l'auteur dans le service), testée par des tests d'intégration.

**Comment gérez-vous l'évolution de la base ?**
Avec Flyway : chaque changement est un fichier `V5__...sql` versionné et appliqué automatiquement au
démarrage. On ne modifie jamais une migration déjà appliquée. Hibernate ne fait que vérifier (`validate`).

**Vos tests tournent sur H2, pas PostgreSQL. Est-ce une limite ?**
Oui : H2 en mode PostgreSQL couvre la logique, pas toutes les spécificités de PostgreSQL. Le schéma
réel (Flyway) est validé au démarrage sur PostgreSQL, et la stack Docker de production a été testée.
L'amélioration serait Testcontainers (un vrai PostgreSQL dans les tests).

**Et le RGPD ?**
Données minimales (nom, email ; email seul pour la newsletter). L'inscription à la newsletter ne
révèle pas si un email existe. L'admin peut supprimer un compte ou une inscription (droit à
l'effacement). Les mots de passe sont hachés. À ajouter avant une vraie mise en ligne : mentions légales,
politique de confidentialité, lien de désinscription dans chaque email.

**Pourquoi avoir réécrit le projet Laravel ?**
Pour le titre CDA, et pour appliquer une architecture en couches plus stricte : typage Java, DTO,
migrations versionnées, sécurité déclarative. J'en ai profité pour corriger des défauts de la première
version (README : « Points de vigilance corrigés »).

**Comment mettriez-vous l'application en ligne ?**
`DEPLOIEMENT.md` : un VPS Ubuntu, Docker, `.env` avec des secrets générés sur le serveur, Caddy pour le
HTTPS automatique, sauvegarde quotidienne de la base et des images (restauration testée), mise à jour
par `git pull` + `docker compose up -d --build` après une CI verte.

**Une difficulté rencontrée ?**
Exemples concrets :
- Un secret généré sous Windows contenait un `\r` invisible et la connexion admin échouait en
  production. Le validateur de configuration refuse maintenant ce cas, avec un message clair.
- Le droit du rédacteur à modifier les articles des autres, trouvé en préparant cette démo et corrigé avec un test.
- La justification du texte, illisible sur téléphone, corrigée après un contrôle à 390 px.
