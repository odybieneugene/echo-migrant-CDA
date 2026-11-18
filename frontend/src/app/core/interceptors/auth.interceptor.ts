import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Ajoute le token JWT aux requêtes ; si l'API répond 401 alors qu'un token a été envoyé,
 * la session n'est plus valable (expirée, compte supprimé...) : déconnexion et retour au login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  if (token) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req).pipe(
    catchError((err: unknown) => {
      // Un 401 sur /auth/login = mauvais identifiants : c'est au formulaire de l'afficher
      if (err instanceof HttpErrorResponse && err.status === 401 && token && !req.url.endsWith('/auth/login')) {
        authService.logout();
        router.navigate(['/login'], { queryParams: { session: 'expiree' } });
      }
      return throwError(() => err);
    })
  );
};
