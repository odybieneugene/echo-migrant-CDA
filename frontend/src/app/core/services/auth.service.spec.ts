import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

/** Fabrique un JWT (non signé : seule la partie centrale est lue côté front). */
function jeton(payload: object): string {
  const base64url = (s: string) => btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url('{"alg":"HS512"}')}.${base64url(JSON.stringify(payload))}.signature`;
}

describe('AuthService', () => {
  const maintenantEnSecondes = () => Math.floor(Date.now() / 1000);

  afterEach(() => localStorage.clear());

  describe('isExpired', () => {
    it('jeton valide encore une heure -> non expiré', () => {
      expect(AuthService.isExpired(jeton({ sub: 'a@b.fr', exp: maintenantEnSecondes() + 3600 }))).toBeFalse();
    });

    it('jeton expiré depuis une minute -> expiré', () => {
      expect(AuthService.isExpired(jeton({ sub: 'a@b.fr', exp: maintenantEnSecondes() - 60 }))).toBeTrue();
    });

    it('jeton illisible -> considéré comme expiré', () => {
      expect(AuthService.isExpired('pas-un-jwt')).toBeTrue();
    });
  });

  it('au démarrage, un jeton expiré est retiré du stockage et la session est fermée', () => {
    localStorage.setItem('echo_migrant_token', jeton({ sub: 'a@b.fr', exp: maintenantEnSecondes() - 60 }));
    localStorage.setItem('echo_migrant_user', JSON.stringify({ id: 1, nom: 'N', prenom: 'P', email: 'a@b.fr', roles: ['admin'] }));

    TestBed.configureTestingModule({ providers: [provideHttpClient()] });
    const service = TestBed.inject(AuthService);

    expect(service.isLoggedIn()).toBeFalse();
    expect(service.currentUser()).toBeNull();
    expect(service.hasRole('admin')).toBeFalse();
    expect(localStorage.getItem('echo_migrant_token')).toBeNull();
  });

  it('au démarrage, un jeton valide garde la session ouverte', () => {
    localStorage.setItem('echo_migrant_token', jeton({ sub: 'a@b.fr', exp: maintenantEnSecondes() + 3600 }));
    localStorage.setItem('echo_migrant_user', JSON.stringify({ id: 1, nom: 'N', prenom: 'P', email: 'a@b.fr', roles: ['journaliste'] }));

    TestBed.configureTestingModule({ providers: [provideHttpClient()] });
    const service = TestBed.inject(AuthService);

    expect(service.isLoggedIn()).toBeTrue();
    expect(service.hasRole('journaliste')).toBeTrue();
    expect(service.hasRole('admin')).toBeFalse();
  });
});
