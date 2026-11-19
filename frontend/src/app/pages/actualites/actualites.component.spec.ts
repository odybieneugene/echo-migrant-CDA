import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActualitesComponent } from './actualites.component';
import { Article } from '../../core/models/models';

const CATEGORIES = [
  { id: 1, nom: 'Actualités' },
  { id: 2, nom: 'Intégration' }
];

const ARTICLES = [
  { id: 10, titre: 'Article actu', categorie: CATEGORIES[0] },
  { id: 11, titre: 'Article intégration', categorie: CATEGORIES[1] },
  { id: 12, titre: 'Article sans catégorie' }
] as Article[];

describe('ActualitesComponent (filtre par catégorie depuis l’URL)', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'actualites', component: ActualitesComponent }]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  /** Ouvre la page à l'URL donnée et répond aux deux appels API. */
  async function ouvrir(url: string): Promise<ActualitesComponent> {
    const page = await harness.navigateByUrl(url, ActualitesComponent);
    http.expectOne((r) => r.url.endsWith('/categories')).flush(CATEGORIES);
    http.expectOne((r) => r.url.endsWith('/articles')).flush(ARTICLES);
    harness.detectChanges();
    return page;
  }

  const titres = (page: ActualitesComponent) => page.articlesFiltres.map((a) => a.titre);
  const titrePage = () => (harness.routeNativeElement?.querySelector('h1')?.textContent ?? '').trim();

  it('sans paramètre : tous les articles, titre « Tous les articles »', async () => {
    const page = await ouvrir('/actualites');

    expect(page.categorieFiltre).toBeNull();
    expect(titres(page).length).toBe(3);
    expect(titrePage()).toBe('Tous les articles');
  });

  it('?categorie=2 : seuls les articles de cette catégorie, titre = nom de la catégorie', async () => {
    const page = await ouvrir('/actualites?categorie=2');

    expect(page.categorieFiltre).toBe(2);
    expect(titres(page)).toEqual(['Article intégration']);
    expect(titrePage()).toBe('Intégration');
  });

  it('paramètre invalide (?categorie=abc) : pas de filtre', async () => {
    const page = await ouvrir('/actualites?categorie=abc');

    expect(page.categorieFiltre).toBeNull();
    expect(titres(page).length).toBe(3);
  });

  it('changer de catégorie dans le menu met à jour la page sans la recharger', async () => {
    const page = await ouvrir('/actualites?categorie=1');
    expect(titres(page)).toEqual(['Article actu']);

    // Même composant, seul le paramètre change : aucun nouvel appel API attendu.
    const memePage = await harness.navigateByUrl('/actualites?categorie=2', ActualitesComponent);

    expect(memePage).toBe(page);
    expect(titres(page)).toEqual(['Article intégration']);
  });

  it('les boutons de filtre écrivent la catégorie dans l’URL (lien partageable)', async () => {
    const page = await ouvrir('/actualites');
    const router = TestBed.inject(Router);

    page.filtrerPar(2);
    await harness.fixture.whenStable();
    expect(router.url).toBe('/actualites?categorie=2');

    page.filtrerPar(null);
    await harness.fixture.whenStable();
    expect(router.url).toBe('/actualites');
  });

  it('?q= : recherche dans le titre et le résumé, sans tenir compte des accents ni de la casse', async () => {
    const page = await ouvrir('/actualites?q=INTEGRATION');

    expect(page.recherche).toBe('INTEGRATION');
    expect(titres(page)).toEqual(['Article intégration']);
  });

  it('recherche + catégorie se cumulent ; taper dans le champ met à jour l’URL', async () => {
    const page = await ouvrir('/actualites?categorie=1');
    const router = TestBed.inject(Router);

    page.rechercher('integration');
    await harness.fixture.whenStable();

    expect(router.url).toBe('/actualites?categorie=1&q=integration');
    expect(titres(page)).toEqual([]); // l'article « intégration » n'est pas dans la catégorie 1
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Aucun article ne correspond à « integration »');
  });

  it('un espace tapé entre deux mots n’est pas effacé par la mise à jour de l’URL', async () => {
    const page = await ouvrir('/actualites');

    page.rechercher('article ');
    await harness.fixture.whenStable();

    expect(page.recherche).toBe('article ');
    expect(TestBed.inject(Router).url).toBe('/actualites?q=article');
  });

  it('lireCategorie refuse 0, les négatifs et les décimaux', () => {
    expect(ActualitesComponent.lireCategorie('3')).toBe(3);
    expect(ActualitesComponent.lireCategorie('0')).toBeNull();
    expect(ActualitesComponent.lireCategorie('-1')).toBeNull();
    expect(ActualitesComponent.lireCategorie('1.5')).toBeNull();
    expect(ActualitesComponent.lireCategorie(null)).toBeNull();
  });
});
