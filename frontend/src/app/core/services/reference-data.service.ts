import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NomDescription, TagSeo } from '../models/models';

/** Regroupe l'accès aux référentiels simples (catégories, types, formats, statuts, tags SEO). */
@Injectable({ providedIn: 'root' })
export class ReferenceDataService {
  constructor(private http: HttpClient) {}

  getCategories(): Observable<NomDescription[]> {
    return this.http.get<NomDescription[]>(`${environment.apiUrl}/categories`);
  }

  getTypesArticles(): Observable<NomDescription[]> {
    return this.http.get<NomDescription[]>(`${environment.apiUrl}/types-articles`);
  }

  getFormats(): Observable<NomDescription[]> {
    return this.http.get<NomDescription[]>(`${environment.apiUrl}/formats`);
  }

  getStatuts(): Observable<NomDescription[]> {
    return this.http.get<NomDescription[]>(`${environment.apiUrl}/statuses`);
  }

  getTagsSeo(): Observable<TagSeo[]> {
    return this.http.get<TagSeo[]>(`${environment.apiUrl}/tags-seo`);
  }
}
