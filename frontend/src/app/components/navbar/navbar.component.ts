import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IsActiveMatchOptions, Params, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ReferenceDataService } from '../../core/services/reference-data.service';
import { NomDescription } from '../../core/models/models';

/** Une rubrique du menu principal. */
export interface EntreeMenu {
  libelle: string;
  lien: string;
  /** `{ categorie: id }` pour une rubrique liée à une catégorie ; null sinon. */
  queryParams: Params | null;
  icone?: 'lecture';
}

/**
 * Rubriques de la maquette liées à une catégorie, retrouvée par son nom
 * (catégories garanties par la migration V5__categories_du_menu.sql).
 */
const RUBRIQUES_CATEGORIES: { libelle: string; categorie: string; icone?: 'lecture' }[] = [
  { libelle: 'Actualités', categorie: 'Actualités' },
  { libelle: 'Reportages', categorie: 'Reportages', icone: 'lecture' },
  { libelle: 'Enquêtes', categorie: 'Enquêtes' }
];

/** « Enquêtes », « enquetes », « ENQUÊTES » -> « enquetes » : comparaison sans casse ni accents. */
function normaliser(nom: string): string {
  return nom.normalize('NFD').replace(/[̀-ͯ]/g, '').trim().toLowerCase();
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit {
  /** Rubriques affichées ; recalculées quand les catégories arrivent de l'API. */
  entreesMenu: EntreeMenu[] = NavbarComponent.construireMenu([]);

  /** Menu replié sur téléphone (géré par Angular : il se referme quand on choisit une rubrique). */
  menuOuvert = false;

  /**
   * Une rubrique n'est « active » que si le chemin ET la catégorie correspondent exactement :
   * sans ça, toutes les rubriques pointant vers /actualites seraient en rose en même temps.
   */
  readonly correspondanceExacte: IsActiveMatchOptions = {
    paths: 'exact',
    queryParams: 'exact',
    matrixParams: 'ignored',
    fragment: 'ignored'
  };

  constructor(
    public authService: AuthService,
    private router: Router,
    private referenceDataService: ReferenceDataService
  ) {}

  ngOnInit(): void {
    // Non bloquant : si l'API ne répond pas, les rubriques mènent à la liste de tous les articles.
    this.referenceDataService.getCategories().subscribe({
      next: (categories) => (this.entreesMenu = NavbarComponent.construireMenu(categories)),
      error: () => {}
    });
  }

  /**
   * « À la Une » (accueil) puis les rubriques liées à une catégorie.
   * Catégorie introuvable en base -> la rubrique mène à tous les articles plutôt qu'à une page vide.
   */
  static construireMenu(categories: NomDescription[]): EntreeMenu[] {
    const parNom = new Map(categories.map((c) => [normaliser(c.nom), c.id]));
    return [
      { libelle: 'À la Une', lien: '/', queryParams: null },
      ...RUBRIQUES_CATEGORIES.map(({ libelle, categorie, icone }) => {
        const id = parNom.get(normaliser(categorie));
        return { libelle, lien: '/actualites', queryParams: id ? { categorie: id } : null, icone };
      })
    ];
  }

  fermerMenu(): void {
    this.menuOuvert = false;
  }

  /** « S'abonner » : formulaire newsletter en bas de l'accueil (défilement même si on y est déjà). */
  sAbonner(): void {
    this.fermerMenu();
    this.router.navigate(['/'], { fragment: 'newsletter' }).then(() =>
      document.getElementById('newsletter')?.scrollIntoView({ behavior: 'smooth' })
    );
  }

  logout(): void {
    this.fermerMenu();
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
