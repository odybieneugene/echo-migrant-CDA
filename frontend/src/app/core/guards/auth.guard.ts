import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Exige d'être connecté, quel que soit le rôle (ex. page "Mon compte"). */
export const authGuard: CanActivateFn = () => {
  if (inject(AuthService).isLoggedIn()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login']);
};
