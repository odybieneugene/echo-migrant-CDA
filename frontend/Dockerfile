# Image de production du frontend : build Angular, puis fichiers statiques servis par nginx.
# nginx joue aussi le rôle de reverse proxy : /api et /uploads sont transmis au backend,
# le navigateur ne parle qu'à un seul domaine (pas de CORS, pas d'URL d'API en dur).

# --- Étape 1 : build ---
FROM node:20-alpine AS build
WORKDIR /app
# Dépendances d'abord : cette couche reste en cache tant que package*.json ne change pas
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# --- Étape 2 : exécution (aucun outil de build, image légère) ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
