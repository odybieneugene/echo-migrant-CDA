# Déploiement en production — Echo Migrant

Guide pour mettre Echo Migrant en ligne sur un serveur Linux (VPS), en HTTPS, avec sauvegardes.
Tout ce qui est décrit ici a été **testé sur la machine de développement** : construction des images,
démarrage de la stack de production sur une base vierge, refus des secrets faibles, parcours complet
via nginx, sauvegarde et restauration de la base et des images (voir [§ 10](#10-ce-qui-a-été-vérifié)).
Seules les parties propres à un serveur réel (DNS, certificat HTTPS, pare-feu) n'ont pas pu l'être.

## Sommaire
1. [Architecture de production](#1-architecture-de-production)
2. [Prérequis](#2-prérequis)
3. [Préparer le serveur](#3-préparer-le-serveur)
4. [Installer l'application](#4-installer-lapplication)
5. [HTTPS avec Caddy](#5-https-avec-caddy)
6. [Mise en service : checklist](#6-mise-en-service--checklist)
7. [Sauvegardes et restauration](#7-sauvegardes-et-restauration)
8. [Mettre à jour](#8-mettre-à-jour)
9. [Surveillance et dépannage](#9-surveillance-et-dépannage)
10. [Ce qui a été vérifié](#10-ce-qui-a-été-vérifié)
11. [Évolutions prévues](#11-évolutions-prévues)

---

## 1. Architecture de production

```
                 Internet
                    │  HTTPS (443)          HTTP (80) → redirigé vers HTTPS
                    ▼
   ┌─────────────────────────────────────┐
   │  Caddy (sur le serveur)             │  certificat Let's Encrypt automatique
   │  reverse proxy HTTPS                │
   └─────────────────┬───────────────────┘
                     │ http://127.0.0.1:8080   (port invisible depuis internet)
   ┌─────────────────▼───────────────────────────────────────────────┐
   │  Docker — docker-compose.prod.yml                               │
   │                                                                 │
   │   frontend (nginx)  ── /api, /uploads ──►  backend (Spring Boot)│
   │   Angular compilé                          profil "prod"        │
   │                                                  │              │
   │                                                  ▼              │
   │                                           postgres (16)         │
   │   volumes : pg_data (base), uploads (images des articles)       │
   └─────────────────────────────────────────────────────────────────┘
```

| Composant | Rôle | Exposé ? |
|---|---|---|
| Caddy | HTTPS, certificat, redirection HTTP → HTTPS | ports 80 et 443 |
| frontend (nginx) | sert l'application Angular, relaie `/api` et `/uploads` | `127.0.0.1:8080` seulement |
| backend (Spring Boot) | API REST, sécurité JWT, migrations Flyway | non (réseau Docker interne) |
| postgres | base de données | non (réseau Docker interne) |

**Choix :** un seul domaine pour le front et l'API (pas de CORS, pas d'URL en dur) ; la base et
l'API ne sont jamais joignables directement ; aucun secret dans le dépôt Git.

## 2. Prérequis

- Un VPS **Ubuntu 24.04 LTS**, **2 Go de RAM minimum** (4 Go conseillés : la JVM, PostgreSQL et la
  construction des images), 20 Go de disque. Exemples : OVH, Scaleway, Hetzner (≈ 5 à 10 €/mois).
- Un **nom de domaine** avec un enregistrement DNS `A` (et `AAAA` en IPv6) vers l'IP du serveur,
  par exemple `echo-migrant.exemple.fr`.
- Un accès SSH par **clé** (pas par mot de passe).

## 3. Préparer le serveur

Toutes les commandes se lancent **sur le serveur**, en SSH.

```bash
# Mises à jour, y compris automatiques pour les correctifs de sécurité
sudo apt update && sudo apt upgrade -y
sudo apt install -y unattended-upgrades git
sudo dpkg-reconfigure -plow unattended-upgrades

# Utilisateur dédié (ne pas travailler en root)
sudo adduser deploy
sudo usermod -aG sudo deploy
# copier sa clé SSH :  ssh-copy-id deploy@IP_DU_SERVEUR  (depuis son poste)

# SSH : interdire root et les mots de passe (dans /etc/ssh/sshd_config)
#   PermitRootLogin no
#   PasswordAuthentication no
sudo systemctl restart ssh

# Pare-feu : SSH, HTTP et HTTPS uniquement
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

> **Piège Docker / UFW** : un port publié par Docker (`ports:`) **contourne UFW**. C'est pourquoi
> nginx écoute sur `127.0.0.1:8080` (`HTTP_BIND=127.0.0.1` dans `.env`) : seul Caddy, sur la même
> machine, peut l'atteindre. La base et le backend n'ont aucun port publié.

**Docker** (dépôt officiel, [docs.docker.com/engine/install/ubuntu](https://docs.docker.com/engine/install/ubuntu/)) :

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker deploy   # puis se déconnecter / reconnecter
docker compose version           # vérification
```

## 4. Installer l'application

```bash
cd ~
git clone https://github.com/odybieneugene/echo-migrant-CDA.git echo-migrant
cd echo-migrant
```

### Le fichier `.env` (secrets)

```bash
cp .env.example .env
chmod 600 .env          # lisible uniquement par l'utilisateur deploy
nano .env
```

Remplir chaque valeur. Les secrets se génèrent **sur le serveur** (Linux : pas de caractère `\r`
parasite, voir [§ 9](#9-surveillance-et-dépannage)) :

```bash
openssl rand -hex 24       # -> DB_PASSWORD
openssl rand -base64 48    # -> JWT_SECRET
openssl rand -base64 18    # -> ADMIN_DEFAULT_PASSWORD (puis le noter dans un gestionnaire de mots de passe)
```

| Variable | Valeur |
|---|---|
| `PUBLIC_URL` | `https://echo-migrant.exemple.fr` (votre domaine) |
| `HTTP_BIND` / `HTTP_PORT` | `127.0.0.1` / `8080` (derrière Caddy) |
| `DB_NAME`, `DB_USER` | `echo_migrant` |
| `DB_PASSWORD` | généré — **pas** `echo_migrant` |
| `JWT_SECRET` | généré, 32 caractères minimum |
| `ADMIN_DEFAULT_EMAIL` / `ADMIN_DEFAULT_PASSWORD` | premier administrateur (12 caractères minimum) |

Le backend **refuse de démarrer** si une variable manque, si un secret est trop court, est une valeur
de développement connue (`ChangeMoi123!`, `echo_migrant`, clé contenant « changeme »…) ou contient un
caractère invisible. Le message liste tous les problèmes d'un coup.

> ⚠️ Changer `JWT_SECRET` plus tard déconnecte tous les utilisateurs (leurs jetons deviennent invalides).
> Ne **jamais** commiter `.env` (il est dans `.gitignore`).

### Démarrer

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Première fois : 5 à 10 minutes (construction des images). Vérifier :

```bash
docker compose -f docker-compose.prod.yml ps                        # 3 services "Up"
docker compose -f docker-compose.prod.yml logs backend | grep -E "migrations|vérifiée|Started"
#   Successfully applied 4 migrations ...
#   Configuration de production vérifiée : secrets présents et non triviaux
#   Started EchoMigrantApplication in ... seconds
curl -s http://127.0.0.1:8080/api/ping                              # {"message":"API Echo Migrant (Spring Boot) fonctionne ✅"}
```

> Docker Compose lit automatiquement le fichier `.env` du dossier : `--env-file .env` est facultatif ici.
> Raccourci pratique (dans `~/.bashrc`) : `alias dcp='docker compose -f docker-compose.prod.yml'`,
> puis `dcp ps`, `dcp logs -f backend`…

## 5. HTTPS avec Caddy

[Caddy](https://caddyserver.com/docs/install#debian-ubuntu-raspbian) obtient et renouvelle seul le
certificat Let's Encrypt, et redirige HTTP vers HTTPS.

```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update && sudo apt install -y caddy
```

`/etc/caddy/Caddyfile` :

```
echo-migrant.exemple.fr {
    reverse_proxy 127.0.0.1:8080

    # HTTPS obligatoire pendant 1 an pour les navigateurs (après avoir vérifié que tout fonctionne)
    header Strict-Transport-Security "max-age=31536000"
    encode gzip
}
```

```bash
sudo systemctl reload caddy
curl -I https://echo-migrant.exemple.fr        # HTTP/2 200
```

Les en-têtes `X-Forwarded-*` transmis par Caddy puis nginx permettent à Spring Boot de savoir que la
requête d'origine était en HTTPS (`server.forward-headers-strategy: framework` du profil prod).

## 6. Mise en service : checklist

**Avant d'ouvrir le site**
- [ ] `.env` rempli avec des secrets générés, `chmod 600 .env`, absent de Git
- [ ] Les 3 conteneurs sont `Up`, les journaux montrent « Configuration de production vérifiée »
- [ ] `https://` fonctionne, `http://` redirige vers `https://`
- [ ] `http://IP_DU_SERVEUR:8080` **ne répond pas** depuis l'extérieur (nginx en 127.0.0.1)
- [ ] Ports 5432 (base) et 8081 (API) fermés depuis l'extérieur : `nmap -p 5432,8081,8080 IP_DU_SERVEUR`

**Juste après**
- [ ] Se connecter avec le compte admin initial, puis **changer son mot de passe** (« Mon compte »)
- [ ] Créer les comptes de la rédaction et leur donner leur rôle (Dashboard → Utilisateurs)
- [ ] Publier un article avec image : il s'affiche sur l'accueil, l'image se charge
- [ ] Faire une première sauvegarde ([§ 7](#7-sauvegardes-et-restauration)) et **tester sa restauration**

> Les données de démonstration (`scripts/demo/`) ne sont **pas** destinées à la production.

## 7. Sauvegardes et restauration

Deux choses à sauvegarder : **la base** (articles, comptes, commentaires…) et **le volume `uploads`**
(images des articles). Les deux commandes ci-dessous ont été testées : la restauration redonne
exactement les mêmes données.

### Sauvegarde manuelle

```bash
mkdir -p ~/sauvegardes && cd ~/echo-migrant
DATE=$(date +%F_%H%M)

# Base (le conteneur reste en marche)
docker compose -f docker-compose.prod.yml exec -T postgres \
  sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner' \
  | gzip > ~/sauvegardes/base_$DATE.sql.gz

# Images des articles
docker run --rm -v echo-migrant-prod_uploads:/app/uploads:ro -v ~/sauvegardes:/sauvegarde \
  alpine tar czf /sauvegarde/uploads_$DATE.tar.gz -C /app uploads
```

### Sauvegarde automatique (tous les jours à 3 h, 14 jours conservés)

`~/sauvegarde.sh` (puis `chmod +x ~/sauvegarde.sh`) :

```bash
#!/bin/sh
set -e
cd ~/echo-migrant
DATE=$(date +%F_%H%M)
docker compose -f docker-compose.prod.yml exec -T postgres \
  sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner' \
  | gzip > ~/sauvegardes/base_$DATE.sql.gz
docker run --rm -v echo-migrant-prod_uploads:/app/uploads:ro -v ~/sauvegardes:/sauvegarde \
  alpine tar czf /sauvegarde/uploads_$DATE.tar.gz -C /app uploads
find ~/sauvegardes -type f -mtime +14 -delete
```

```bash
crontab -e
# ajouter la ligne :
0 3 * * * /home/deploy/sauvegarde.sh >> /home/deploy/sauvegardes/journal.log 2>&1
```

> **Une sauvegarde sur le même serveur ne protège pas d'une panne du serveur.** Copier régulièrement
> `~/sauvegardes` ailleurs : `rsync`/`scp` vers une autre machine, ou un stockage objet (S3, OVH Object
> Storage…) avec `rclone`. Et tester la restauration de temps en temps : une sauvegarde jamais
> restaurée n'est pas une sauvegarde.

### Restauration

```bash
cd ~/echo-migrant
docker compose -f docker-compose.prod.yml stop backend      # plus d'écriture pendant la restauration

# Base : le dump contient des DROP ... IF EXISTS, il remplace les données actuelles
gunzip -c ~/sauvegardes/base_AAAA-MM-JJ_HHMM.sql.gz | docker compose -f docker-compose.prod.yml exec -T postgres \
  sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'

# Images
docker run --rm -v echo-migrant-prod_uploads:/app/uploads -v ~/sauvegardes:/sauvegarde:ro \
  alpine tar xzf /sauvegarde/uploads_AAAA-MM-JJ_HHMM.tar.gz -C /app

docker compose -f docker-compose.prod.yml start backend
```

## 8. Mettre à jour

```bash
cd ~/echo-migrant
~/sauvegarde.sh                                   # toujours sauvegarder avant
git pull
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
docker compose -f docker-compose.prod.yml logs --tail 50 backend
```

- Les **migrations Flyway** (`V5__...sql`…) s'appliquent seules au démarrage du backend.
  Une migration déjà appliquée n'est jamais modifiée : toute évolution du schéma = nouvelle migration.
- Coupure de service : quelques secondes pendant le redémarrage des conteneurs.
- **Retour arrière** : `git checkout <commit précédent>` puis `up -d --build`. Si la mise à jour
  contenait une migration, restaurer aussi la sauvegarde de la base faite juste avant (Flyway ne
  « dé-migre » pas).
- Avant de mettre à jour, vérifier que la CI GitHub Actions est **verte** sur ce commit
  (badge du README).
- Nettoyer de temps en temps les anciennes images : `docker image prune -f`.

## 9. Surveillance et dépannage

```bash
docker compose -f docker-compose.prod.yml ps               # état des conteneurs
docker compose -f docker-compose.prod.yml logs -f backend  # journaux en direct
docker stats --no-stream                                   # mémoire / CPU
df -h                                                      # disque
curl -s https://echo-migrant.exemple.fr/api/ping           # santé de l'API
```

Les journaux Docker sont limités à 3 × 10 Mo par conteneur (`docker-compose.prod.yml`).
Pour être alerté d'une panne : un service de surveillance externe (UptimeRobot, Better Stack…) qui
appelle `/api/ping` toutes les 5 minutes.

| Symptôme | Cause probable | Solution |
|---|---|---|
| `required variable X is missing a value` | variable absente de `.env`, ou commande lancée hors du dossier du projet | la renseigner / `cd ~/echo-migrant` |
| Backend arrêté, `Configuration de production refusée` | secret faible ou de développement | lire la liste affichée, corriger `.env` |
| `... contient un caractère invisible` | `.env` écrit sous Windows (fins de ligne `\r\n`) | `sed -i 's/\r$//' .env` ou régénérer les secrets sur le serveur |
| L'admin initial ne peut pas se connecter | le compte existait déjà : `ADMIN_DEFAULT_PASSWORD` n'est utilisé qu'au **premier** démarrage | se connecter avec l'ancien mot de passe, ou restaurer |
| Page blanche, erreurs 502 sur `/api` | backend encore en démarrage ou arrêté | `logs backend` (démarrage ≈ 20 s) |
| Envoi d'image refusé (413) | image de plus de 5 Mo | réduire l'image |
| Tout le monde est déconnecté | `JWT_SECRET` a changé | normal : se reconnecter |

## 10. Ce qui a été vérifié

Sur la machine de développement (Docker Desktop), avec `docker-compose.prod.yml` et un `.env` de test :

| Vérification | Résultat |
|---|---|
| Construction des images backend et frontend | ✅ (backend 515 Mo, frontend 75 Mo) — aussi refaite par la CI à chaque push |
| Démarrage sur une base vierge | ✅ 4 migrations Flyway appliquées, admin initial créé |
| Profil prod sans variables / avec les secrets de dev / avec un `\r` | ✅ refus, avec le message adapté |
| Accueil, liens directs (`/dashboard`, `/actualites/3`) via nginx | ✅ 200 |
| En-têtes de sécurité sur toutes les réponses, version nginx masquée | ✅ |
| Connexion admin, création d'un article avec image via nginx | ✅ image servie à l'identique |
| Persistance après redémarrage du backend | ✅ article et image conservés |
| Image de 6 Mo | ✅ refusée (413) |
| Backend et base injoignables directement | ✅ |
| Sauvegarde + restauration de la base | ✅ données identiques (articles, comptes, version de schéma) |
| Sauvegarde + restauration du volume des images | ✅ fichiers identiques (empreinte) |

Non vérifiable hors d'un vrai serveur : DNS, certificat Let's Encrypt, pare-feu UFW.

## 11. Évolutions prévues

- **Déploiement continu (CD)** : la CI construit déjà les images ; étape suivante, les publier sur
  GitHub Container Registry et déployer automatiquement sur le serveur (SSH depuis GitHub Actions)
  quand `main` est vert — plus de construction sur le serveur.
- **Sauvegardes hors site** automatiques (rclone vers un stockage objet) avec alerte en cas d'échec.
- **Limitation des tentatives de connexion** (anti force brute) sur `/api/auth/login`.
- **Envoi réel de la newsletter** (service d'emailing) avec lien de désinscription.
- **Surveillance** : métriques Spring Boot Actuator, tableau de bord, alertes.
