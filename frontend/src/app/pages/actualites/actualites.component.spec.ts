import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ActualitesComponent, ARTICLES_PAR_PAGE } from './actualites.component';
import { Article, PageResponse } from '../../core/models/models';

const CATEGORIES = [
  { id: 1, nom: 'Actualités' },
  { id: 2, nom: 'Intégration' }
];

const ARTICLES = [
  { id: 10, titre: 'Article actu', categorie: CATEGORIES[0] },
  { id: 11, titre: 'Article intégration', categorie: CATEGORIES[1] },
  { id: 12, titre: 'Article sans catégorie' }
] as Article[];

function unePage(contenu: Article[], page = 0, totalPages = 1, totalElements = contenu.length): PageResponse<Article> {
  return { contenu, page, taille: ARTICLES_PAR_PAGE, totalElements, totalPages };
}

describe('ActualitesComponent (filtres et pagination depuis l’URL)', () => {
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
    spyOn(window, 'scrollTo');
  });

  afterEach(() => http.verify());

  const requeteArticles = (): TestRequest => http.expectOne((r) => r.url.endsWith('/articles/recherche'));

  /** Ouvre la page à l'URL donnée, répond aux catégories et renvoie la requête d'articles (à vérifier puis flush). */
  async function ouvrir(url: string): Promise<{ page: ActualitesComponent; requete: TestRequest }> {
    const page = await harness.navigateByUrl(url, ActualitesComponent);
    http.expectOne((r) => r.url.endsWith('/categories')).flush(CATEGORIES);
    return { page, requete: requeteArticles() };
  }

  const param = (req: TestRequest, nom: string) => req.request.params.get(nom);
  const texte = () => harness.routeNativeElement?.textContent ?? '';
  const titrePage = () => (harness.routeNativeElement?.querySelector('h1')?.textContent ?? '').trim();

  it('sans paramètre : première page de tous les articles, titre « Tous les articles »', async () => {
    const { page, requete } = await ouvrir('/actualites');

    expect(param(requete, 'page')).toBe('0');
    expect(param(requete, 'taille')).toBe(String(ARTICLES_PAR_PAGE));
    expect(param(requete, 'categorie')).toBeNull();
    expect(param(requete, 'q')).toBeNull();
    requete.flush(unePage(ARTICLES));
    harness.detectChanges();

    expect(page.articles.length).toBe(3);
    expect(titrePage()).toBe('Tous les articles');
    expect(harness.routeNativeElement?.querySelector('nav[aria-label="Pages des articles"]')).toBeNull();
  });

  it('?categorie=2&q=INTEGRATION&page=3 : critères transmis à l’API (page comptée à partir de 0)', async () => {
    const { page, requete } = await ouvrir('/actualites?categorie=2&q=INTEGRATION&page=3');

    expect(param(requete, 'categorie')).toBe('2');
    expect(param(requete, 'q')).toBe('INTEGRATION');
    expect(param(requete, 'page')).toBe('2');
    requete.flush(unePage([ARTICLES[1]], 2, 3, 25));
    harness.detectChanges();

    expect(page.recherche).toBe('INTEGRATION');
    expect(titrePage()).toBe('Intégration');
    expect(texte()).toContain('25 articles');
    expect(texte()).toContain('page 3 sur 3');
  });

  it('paramètres invalides (?categorie=abc&page=0) : pas de filtre, première page', async () => {
    const { page, requete } = await ouvrir('/actualites?categorie=abc&page=0');

    expect(param(requete, 'categorie')).toBeNull();
    expect(param(requete, 'page')).toBe('0');
    requete.flush(unePage(ARTICLES));
    expect(page.categorieFiltre).toBeNull();
  });

  it('changer de catégorie dans le menu recharge les articles sans recréer la page', async () => {
    const { page, requete } = await ouvrir('/actualites?categorie=1');
    requete.flush(unePage([ARTICLES[0]]));

    const memePage = await harness.navigateByUrl('/actualites?categorie=2', ActualitesComponent);
    const nouvelle = requeteArticles();
    expect(param(nouvelle, 'categorie')).toBe('2');
    nouvelle.flush(unePage([ARTICLES[1]]));

    expect(memePage).toBe(page);
    expect(page.articles.map((a) => a.titre)).toEqual(['Article intégration']);
  });

  it('les boutons de filtre écrivent la catégorie dans l’URL et reviennent à la première page', async () => {
    const { page, requete } = await ouvrir('/actualites?page=3');
    requete.flush(unePage(ARTICLES, 2, 3, 30));
    const router = TestBed.inject(Router);

    page.filtrerPar(2);
    await harness.fixture.whenStable();
    expect(router.url).toBe('/actualites?categorie=2');
    requeteArticles().flush(unePage([ARTICLES[1]]));

    page.filtrerPar(null);
    await harness.fixture.whenStable();
    expect(router.url).toBe('/actualites');
    requeteArticles().flush(unePage(ARTICLES));
  });

  it('taper dans la recherche met à jour l’URL (sans la page) ; aucun résultat -> message', async () => {
    const { page, requete } = await ouvrir('/actualites?categorie=1&page=2');
    requete.flush(unePage([ARTICLES[0]], 1, 2, 13));
    const router = TestBed.inject(Router);

    page.rechercher('integration');
    await harness.fixture.whenStable();

    expect(router.url).toBe('/actualites?categorie=1&q=integration');
    const req = requeteArticles();
    expect(param(req, 'q')).toBe('integration');
    expect(param(req, 'page')).toBe('0');
    req.flush(unePage([], 0, 0, 0));
    harness.detectChanges();
    expect(texte()).toContain('Aucun article ne correspond à « integration »');
  });

  it('un espace tapé entre deux mots n’est pas effacé par la mise à jour de l’URL', async () => {
    const { page, requete } = await ouvrir('/actualites');
    requete.flush(unePage(ARTICLES));

    page.rechercher('article ');
    await harness.fixture.whenStable();

    expect(page.recherche).toBe('article ');
    expect(TestBed.inject(Router).url).toBe('/actualites?q=article');
    expect(param(requeteArticles(), 'q')).toBe('article');
  });

  it('plusieurs pages : liens numérotés, page courante marquée, liens précédent et suivant', async () => {
    const { requete } = await ouvrir('/actualites?q=migration&page=2');
    requete.flush(unePage(ARTICLES, 1, 4, 40));
    harness.detectChanges();

    const nav = harness.routeNativeElement!.querySelector('nav[aria-label="Pages des articles"]')!;
    expect(nav.querySelector('[aria-current="page"]')!.textContent?.trim()).toBe('2');
    expect(nav.querySelector('a[aria-label="Page suivante"]')!.getAttribute('href')).toBe('/actualites?q=migration&page=3');
    // La page 1 n'apparaît pas dans l'URL
    expect(nav.querySelector('a[aria-label="Page précédente"]')!.getAttribute('href')).toBe('/actualites?q=migration');
  });

  it('suivre un lien de page charge la page demandée et remonte en haut', async () => {
    const { requete } = await ouvrir('/actualites');
    requete.flush(unePage(ARTICLES, 0, 3, 30));

    await harness.navigateByUrl('/actualites?page=2', ActualitesComponent);
    const req = requeteArticles();
    expect(param(req, 'page')).toBe('1');
    req.flush(unePage(ARTICLES, 1, 3, 30));
    expect(window.scrollTo).toHaveBeenCalled();
  });

  it('erreur de l’API : message, pas de plantage', async () => {
    const { requete } = await ouvrir('/actualites');
    requete.flush('erreur', { status: 500, statusText: 'Server Error' });
    harness.detectChanges();

    expect(texte()).toContain('Les articles n\'ont pas pu être chargés');
  });

  it('pagesAAfficher : première, dernière et voisines de la page courante', () => {
    expect(ActualitesComponent.pagesAAfficher(1, 1)).toEqual([1]);
    expect(ActualitesComponent.pagesAAfficher(1, 3)).toEqual([1, 2, 3]);
    expect(ActualitesComponent.pagesAAfficher(6, 12)).toEqual([1, null, 5, 6, 7, null, 12]);
    expect(ActualitesComponent.pagesAAfficher(3, 12)).toEqual([1, 2, 3, 4, null, 12]);
    expect(ActualitesComponent.pagesAAfficher(4, 12)).toEqual([1, 2, 3, 4, 5, null, 12]); // un seul trou : numéro affiché
    expect(ActualitesComponent.pagesAAfficher(1, 0)).toEqual([]);
  });

  it('lireCategorie et lirePage refusent 0, les négatifs et les décimaux', () => {
    expect(ActualitesComponent.lireCategorie('3')).toBe(3);
    expect(ActualitesComponent.lireCategorie('0')).toBeNull();
    expect(ActualitesComponent.lireCategorie('-1')).toBeNull();
    expect(ActualitesComponent.lireCategorie('1.5')).toBeNull();
    expect(ActualitesComponent.lireCategorie(null)).toBeNull();
    expect(ActualitesComponent.lirePage('4')).toBe(4);
    expect(ActualitesComponent.lirePage('0')).toBe(1);
    expect(ActualitesComponent.lirePage('abc')).toBe(1);
    expect(ActualitesComponent.lirePage(null)).toBe(1);
  });
});
