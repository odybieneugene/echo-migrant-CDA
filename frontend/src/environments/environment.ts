// Production (ng build) : l'API est servie sur le même domaine que le front, derrière nginx
// (/api et /uploads redirigés vers le backend, voir nginx.conf) -> URL relative, aucun domaine en dur.
export const environment = {
  production: true,
  apiUrl: '/api'
};
