import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Utilisateur } from '../models/models';

/** Administration des comptes (routes réservées à l'admin côté API). */
@Injectable({ providedIn: 'root' })
export class UtilisateurService {
  private readonly baseUrl = `${environment.apiUrl}/utilisateurs`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(this.baseUrl);
  }

  updateRoles(id: number, roles: string[]): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`${this.baseUrl}/${id}/roles`, { roles });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
