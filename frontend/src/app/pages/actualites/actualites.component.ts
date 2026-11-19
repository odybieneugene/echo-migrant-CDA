import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Article, NomDescription, IMAGE_ARTICLE_PAR_DEFAUT } from '../../core/models/models';
import { ArticleService } from '../../core/services/article.service';
import { ReferenceDataService } from '../../core/services/reference-data.service';

/** Minuscules, sans accents : « Société » est trouvé en tapant « societe ». */
function normaliser(texte: string): string {
  return texte.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

/**
 * Liste des articles publiés, filtrable par catégorie et par recherche.
 * Les deux filtres viennent de l'URL (`/actualites?categorie=3&q=asile`) : les rubriques du menu
 * et les boutons de la page passent tous par là, ce qui rend chaque recherche partageable.
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
  loading = true;

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private articleService: ArticleService,
    private referenceDataService: ReferenceDataService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Le composant n'est pas recréé quand seul le paramètre change (clic dans le menu) : on s'abonne.
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      this.categorieFiltre = ActualitesComponent.lireCategorie(params.get('categorie'));
      // L'URL contient la recherche sans espaces de fin : ne pas réécrire le champ pendant la frappe
      // (sinon l'espace tapé entre deux mots disparaîtrait aussitôt).
      const q = params.get('q') ?? '';
      if (q !== this.recherche.trim()) {
        this.recherche = q;
      }
    });

    // Loupe du menu (/actualites#recherche) : on place le curseur dans le champ de recherche.
    this.route.fragment.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((ancre) => {
      if (ancre === 'recherche') {
        setTimeout(() => document.getElementById('recherche')?.focus());
      }
    });

    this.referenceDataService.getCategories().subscribe((categories) => (this.categories = categories));
    this.articleService.getPublies().subscribe({
      next: (articles) => {
        this.articles = articles;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  /** `?categorie=abc`, `0` ou négatif -> pas de filtre (toutes les catégories). */
  static lireCategorie(valeur: string | null): number | null {
    const id = Number(valeur);
    return valeur && Number.isInteger(id) && id > 0 ? id : null;
  }

  get articlesFiltres(): Article[] {
    const mots = normaliser(this.recherche).split(/\s+/).filter(Boolean);
    return this.articles.filter((a) => {
      if (this.categorieFiltre && a.categorie?.id !== this.categorieFiltre) {
        return false;
      }
      // Tous les mots tapés doivent apparaître dans le titre ou le résumé (dans n'importe quel ordre).
      const texte = normaliser(`${a.titre} ${a.resume ?? ''}`);
      return mots.every((mot) => texte.includes(mot));
    });
  }

  /** Catégorie filtrée, pour le titre de la page (null tant que les catégories ne sont pas chargées). */
  get categorieCourante(): NomDescription | null {
    return this.categories.find((c) => c.id === this.categorieFiltre) ?? null;
  }

  filtrerPar(categorieId: number | null): void {
    this.mettreAJourUrl({ categorie: categorieId });
  }

  rechercher(texte: string): void {
    this.recherche = texte;
    this.mettreAJourUrl({ q: texte.trim() || null });
  }

  /** Paramètre à null -> retiré de l'URL. replaceUrl : pas une entrée d'historique par lettre tapée. */
  private mettreAJourUrl(params: { categorie?: number | null; q?: string | null }): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }
}
