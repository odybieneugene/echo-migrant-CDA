import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Article, ArticleRequest, CriteresRecherche, PageResponse } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly baseUrl = `${environment.apiUrl}/articles`;

  constructor(private http: HttpClient) {}

  getPublies(limit?: number): Observable<Article[]> {
    const params = limit ? { limit: String(limit) } : undefined;
    return this.http.get<Article[]>(this.baseUrl, { params });
  }

  /** Articles publiés page par page, filtrés côté serveur (catégorie, mots cherchés). */
  rechercher(criteres: CriteresRecherche): Observable<PageResponse<Article>> {
    let params = new HttpParams().set('page', criteres.page).set('taille', criteres.taille);
    if (criteres.categorie) {
      params = params.set('categorie', criteres.categorie);
    }
    if (criteres.q) {
      params = params.set('q', criteres.q);
    }
    return this.http.get<PageResponse<Article>>(`${this.baseUrl}/recherche`, { params });
  }

  getAllAdmin(): Observable<Article[]> {
    return this.http.get<Article[]>(`${this.baseUrl}/admin`);
  }

  getById(id: number): Observable<Article> {
    return this.http.get<Article>(`${this.baseUrl}/${id}`);
  }

  create(data: ArticleRequest, image?: File | null): Observable<Article> {
    return this.http.post<Article>(this.baseUrl, this.buildFormData(data, image));
  }

  update(id: number, data: ArticleRequest, image?: File | null): Observable<Article> {
    return this.http.put<Article>(`${this.baseUrl}/${id}`, this.buildFormData(data, image));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  private buildFormData(data: ArticleRequest, image?: File | null): FormData {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (image) {
      formData.append('image', image);
    }
    return formData;
  }
}
