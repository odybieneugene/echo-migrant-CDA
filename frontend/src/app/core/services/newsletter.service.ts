import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { InscriptionNewsletter } from '../models/models';

@Injectable({ providedIn: 'root' })
export class NewsletterService {
  private readonly baseUrl = `${environment.apiUrl}/newsletter`;

  constructor(private http: HttpClient) {}

  /** Public. Même réponse que l'email soit nouveau ou déjà inscrit. */
  inscrire(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(this.baseUrl, { email });
  }

  /** Admin uniquement. */
  getAll(): Observable<InscriptionNewsletter[]> {
    return this.http.get<InscriptionNewsletter[]>(this.baseUrl);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
