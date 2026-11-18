import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Commentaire } from '../models/models';

@Injectable({ providedIn: 'root' })
export class CommentaireService {
  private readonly baseUrl = `${environment.apiUrl}/commentaires`;

  constructor(private http: HttpClient) {}

  getByArticle(articleId: number): Observable<Commentaire[]> {
    return this.http.get<Commentaire[]>(this.baseUrl, { params: { articleId: String(articleId) } });
  }

  create(articleId: number, contenu: string): Observable<Commentaire> {
    return this.http.post<Commentaire>(this.baseUrl, { articleId, contenu });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
