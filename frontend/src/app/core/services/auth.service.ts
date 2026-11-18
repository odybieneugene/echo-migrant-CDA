import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, Utilisateur } from '../models/models';

const TOKEN_KEY = 'echo_migrant_token';
const USER_KEY = 'echo_migrant_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // L'ordre compte : readValidToken() nettoie le stockage si le token a expiré, avant la lecture de l'utilisateur
  private readonly tokenSignal = signal<string | null>(this.readValidToken());
  private readonly currentUserSignal = signal<Utilisateur | null>(this.tokenSignal() ? this.readStoredUser() : null);

  readonly currentUser = computed(() => this.currentUserSignal());
  readonly isLoggedIn = computed(() => !!this.tokenSignal());

  constructor(private http: HttpClient) {}

  register(payload: { nom: string; prenom: string; email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload)
      .pipe(tap((res) => this.setSession(res)));
  }

  login(payload: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, payload)
      .pipe(tap((res) => this.setSession(res)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.tokenSignal.set(null);
    this.currentUserSignal.set(null);
  }

  getToken(): string | null {
    return this.tokenSignal();
  }

  hasRole(role: string): boolean {
    const user = this.currentUserSignal();
    return !!user?.roles?.some((r) => r.toLowerCase() === role.toLowerCase());
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some((r) => this.hasRole(r));
  }

  private setSession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this.tokenSignal.set(res.token);
    this.currentUserSignal.set(res.user);
  }

  /** Token stocké s'il est encore valide ; un token expiré (ou illisible) est supprimé. */
  private readValidToken(): string | null {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token && AuthService.isExpired(token)) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      return null;
    }
    return token;
  }

  /** Lit la date d'expiration ("exp", en secondes) dans la partie centrale du JWT. */
  static isExpired(token: string): boolean {
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const { exp } = JSON.parse(atob(payload));
      return typeof exp === 'number' && exp * 1000 <= Date.now();
    } catch {
      return true; // token mal formé : on le considère comme invalide
    }
  }

  private readStoredUser(): Utilisateur | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) as Utilisateur : null;
  }
}
