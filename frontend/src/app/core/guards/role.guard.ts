import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Équivalent de ProtectedRoute.jsx : exige d'être connecté ET d'avoir un rôle éditorial. */
export const roleGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn() && authService.hasAnyRole(['admin', 'journaliste', 'redacteur'])) {
    return true;
  }

  router.navigate(authService.isLoggedIn() ? ['/'] : ['/login']);
  return false;
};
