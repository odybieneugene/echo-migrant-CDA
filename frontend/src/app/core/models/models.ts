export interface Utilisateur {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  roles: string[];
}

export interface AuthResponse {
  token: string;
  user: Utilisateur;
}

export interface NomDescription {
  id: number;
  nom: string;
  description?: string;
}

export interface TagSeo {
  id: number;
  motCle: string;
}

export interface Article {
  id: number;
  titre: string;
  resume?: string;
  contenu?: string;
  imageCouverture?: string;
  metaTitre?: string;
  metaDescription?: string;
  metaKeywords?: string;
  estEnVedette: boolean;
  categorie?: NomDescription;
  typeArticle?: NomDescription;
  format?: NomDescription;
  statut?: NomDescription;
  auteurId?: number;
  auteurNomComplet?: string;
  tagsSeo: TagSeo[];
  createdAt: string;
  updatedAt: string;
}

/** Une page de résultats de l'API (`PageResponse` côté backend). `page` commence à 0. */
export interface PageResponse<T> {
  contenu: T[];
  page: number;
  taille: number;
  totalElements: number;
  totalPages: number;
}

/** Critères de GET /api/articles/recherche. */
export interface CriteresRecherche {
  categorie?: number | null;
  q?: string | null;
  page: number;
  taille: number;
}

export interface ArticleRequest {
  titre: string;
  resume?: string;
  contenu?: string;
  categorieId?: number;
  typeArticleId?: number;
  formatId?: number;
  statutId?: number;
  metaTitre?: string;
  metaDescription?: string;
  metaKeywords?: string;
  estEnVedette?: boolean;
  tagSeoIds?: number[];
}

export interface Commentaire {
  id: number;
  contenu: string;
  articleId: number;
  utilisateurId: number;
  utilisateurNomComplet: string;
  createdAt: string;
}

export interface InscriptionNewsletter {
  id: number;
  email: string;
  createdAt: string;
}

/** Visuel des articles sans image de couverture (fichier local dans public/img). */
export const IMAGE_ARTICLE_PAR_DEFAUT = '/img/article-par-defaut.svg';
