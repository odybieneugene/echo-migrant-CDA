import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap } from 'rxjs';
import { Article, NomDescription, IMAGE_ARTICLE_PAR_DEFAUT } from '../../core/models/models';
import { ArticleService } from '../../core/services/article.service';
import { ReferenceDataService } from '../../core/services/reference-data.service';

/** Nombre d'articles par page (grille de 3 colonnes : 4 lignes). */
export const ARTICLES_PAR_PAGE = 12;

/**
 * Liste des articles publiés, page par page, filtrable par catégorie et par recherche.
 * Tout vient de l'URL (`/actualites?categorie=3&q=asile&page=2`) : les rubriques du menu, les filtres
 * et la pagination passent par là, ce qui rend chaque page partageable et le bouton « Précédent » utilisable.
 * Le filtrage et le découpage sont faits par l'API (GET /api/articles/recherche) : le navigateur
 * ne reçoit que les articles de la page affichée.
 */
@Component({
  selector: 'app-actualites',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './actualites.component.html'
})
export class ActualitesComponent implements OnInit {
  readonly imageParDefaut = IMAGE_ARTICLE_PAR_DEFAUT;
  articles: Article[] = [];
  categories: NomDescription[] = [];
  categorieFiltre: number | null = null;
  recherche = '';
  /** Page courante, à partir de 1 (comme dans l'URL). */
  page = 1;
  totalPages = 0;
  totalArticles = 0;
  loading = true;
  erreur = false;

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private articleService: ArticleService,
    private referenceDataService: ReferenceDataService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Le composant n'est pas recréé quand seuls les paramètres changent (menu, filtres, pages) : on s'abonne.
    // switchMap annule la requête précédente si l'URL change avant sa réponse (frappe dans la recherche).
    this.route.queryParamMap
      .pipe(
        tap((params) => this.lireUrl(params)),
        switchMap(() => {
          this.loading = true;
          this.erreur = false;
          return this.articleService
            .rechercher({
              categorie: this.categorieFiltre,
              q: this.recherche.trim() || null,
              page: this.page - 1, // l'API compte à partir de 0
              taille: ARTICLES_PAR_PAGE
            })
            .pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((resultat) => {
        this.loading = false;
        this.erreur = resultat === null;
        this.articles = resultat?.contenu ?? [];
        this.totalPages = resultat?.totalPages ?? 0;
        this.totalArticles = resultat?.totalElements ?? 0;
      });

    // Loupe du menu (/actualites#recherche) : on place le curseur dans le champ de recherche.
    this.route.fragment.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((ancre) => {
      if (ancre === 'recherche') {
        setTimeout(() => document.getElementById('recherche')?.focus());
      }
    });

    this.referenceDataService.getCategories().subscribe((categories) => (this.categories = categories));
  }

  private lireUrl(params: ParamMap): void {
    const pagePrecedente = this.page;
    this.categorieFiltre = ActualitesComponent.lireCategorie(params.get('categorie'));
    this.page = ActualitesComponent.lirePage(params.get('page'));
    // L'URL contient la recherche sans espaces de fin : ne pas réécrire le champ pendant la frappe
    // (sinon l'espace tapé entre deux mots disparaîtrait aussitôt).
    const q = params.get('q') ?? '';
    if (q !== this.recherche.trim()) {
      this.recherche = q;
    }
    // Nouvelle page : on remonte en haut de la liste (pas à chaque lettre tapée)
    if (this.page !== pagePrecedente) {
      window.scrollTo({ top: 0 });
    }
  }

  /** `?categorie=abc`, `0` ou négatif -> pas de filtre (toutes les catégories). */
  static lireCategorie(valeur: string | null): number | null {
    const id = Number(valeur);
    return valeur && Number.isInteger(id) && id > 0 ? id : null;
  }

  /** `?page=abc`, `0` ou négatif -> première page. */
  static lirePage(valeur: string | null): number {
    const page = Number(valeur);
    return valeur && Number.isInteger(page) && page > 0 ? page : 1;
  }

  /**
   * Numéros de page à afficher : la première, la dernière et les voisines de la page courante ;
   * `null` marque un saut (« … »). Ex. page 6 sur 12 -> 1 … 5 6 7 … 12.
   */
  static pagesAAfficher(courante: number, total: number): (number | null)[] {
    const pages = [...new Set([1, courante - 1, courante, courante + 1, total])]
      .filter((p) => p >= 1 && p <= total)
      .sort((a, b) => a - b);
    const resultat: (number | null)[] = [];
    pages.forEach((p, i) => {
      if (i > 0 && p - pages[i - 1] > 1) {
        resultat.push(p - pages[i - 1] === 2 ? p - 1 : null); // un seul numéro manquant : on l'affiche
      }
      resultat.push(p);
    });
    return resultat;
  }

  get pages(): (number | null)[] {
    return ActualitesComponent.pagesAAfficher(this.page, this.totalPages);
  }

  /** Paramètres d'URL d'une page : la page 1 n'apparaît pas dans l'URL. */
  parametresPage(page: number): { page: number | null } {
    return { page: page > 1 ? page : null };
  }

  /** Catégorie filtrée, pour le titre de la page (null tant que les catégories ne sont pas chargées). */
  get categorieCourante(): NomDescription | null {
    return this.categories.find((c) => c.id === this.categorieFiltre) ?? null;
  }

  /** Changer de filtre ramène à la première page. */
  filtrerPar(categorieId: number | null): void {
    this.mettreAJourUrl({ categorie: categorieId, page: null });
  }

  rechercher(texte: string): void {
    this.recherche = texte;
    this.mettreAJourUrl({ q: texte.trim() || null, page: null });
  }

  /** Paramètre à null -> retiré de l'URL. replaceUrl : pas une entrée d'historique par lettre tapée. */
  private mettreAJourUrl(params: { categorie?: number | null; q?: string | null; page?: number | null }): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }
}
