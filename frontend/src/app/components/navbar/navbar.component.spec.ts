import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Component } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NavbarComponent } from './navbar.component';

@Component({ standalone: true, imports: [NavbarComponent], template: '<app-navbar></app-navbar>' })
class PageAvecMenu {}

describe('NavbarComponent (menu de la maquette)', () => {
  describe('construireMenu', () => {
    it('sans catégories : « À la Une » puis les trois rubriques vers tous les articles', () => {
      const menu = NavbarComponent.construireMenu([]);

      expect(menu.map((e) => e.libelle)).toEqual(['À la Une', 'Actualités', 'Reportages', 'Enquêtes']);
      expect(menu[0]).toEqual(jasmine.objectContaining({ lien: '/', queryParams: null }));
      expect(menu.slice(1).every((e) => e.lien === '/actualites' && e.queryParams === null)).toBeTrue();
    });

    it('chaque rubrique pointe vers sa catégorie, retrouvée par le nom (casse et accents ignorés)', () => {
      const menu = NavbarComponent.construireMenu([
        { id: 4, nom: 'Intégration' },
        { id: 7, nom: 'actualités' },
        { id: 8, nom: 'Reportages' },
        { id: 12, nom: 'ENQUETES' }
      ]);

      expect(menu[1].queryParams).toEqual({ categorie: 7 });
      expect(menu[2].queryParams).toEqual({ categorie: 8 });
      expect(menu[2].icone).toBe('lecture');
      expect(menu[3].queryParams).toEqual({ categorie: 12 });
    });
  });

  describe('affichage', () => {
    let http: HttpTestingController;
    let harness: RouterTestingHarness;

    beforeEach(async () => {
      localStorage.clear();
      TestBed.configureTestingModule({
        providers: [
          provideRouter([
            { path: '', component: PageAvecMenu },
            { path: 'actualites', component: PageAvecMenu }
          ]),
          provideHttpClient(),
          provideHttpClientTesting()
        ]
      });
      http = TestBed.inject(HttpTestingController);
      harness = await RouterTestingHarness.create();
    });

    afterEach(() => http.verify());

    async function ouvrir(url: string): Promise<HTMLElement> {
      await harness.navigateByUrl(url);
      http.expectOne((r) => r.url.endsWith('/categories')).flush([
        { id: 7, nom: 'Actualités' },
        { id: 8, nom: 'Reportages' }
      ]);
      harness.detectChanges();
      await harness.fixture.whenStable();
      harness.detectChanges();
      return harness.routeNativeElement as HTMLElement;
    }

    const rubriqueActive = (page: HTMLElement) =>
      Array.from(page.querySelectorAll('.em-rubriques .nav-link.active')).map((a) => a.textContent?.trim());

    it('visiteur : liens vers les catégories, « Se connecter » et « S’abonner »', async () => {
      const page = await ouvrir('/');
      const liens = Array.from(page.querySelectorAll<HTMLAnchorElement>('.em-rubriques .nav-link'));

      expect(liens.map((a) => a.getAttribute('href'))).toEqual([
        '/',
        '/actualites?categorie=7',
        '/actualites?categorie=8',
        '/actualites' // « Enquêtes » absente de la réponse : tous les articles
      ]);
      expect(page.textContent).toContain('Se connecter');
      expect(page.textContent).toContain('S’abonner');
      expect(page.textContent).not.toContain('Dashboard');
    });

    it('une seule rubrique en rose : celle de la page affichée', async () => {
      expect(rubriqueActive(await ouvrir('/'))).toEqual(['À la Une']);
    });

    it('sur /actualites?categorie=8, seule « Reportages » est active', async () => {
      expect(rubriqueActive(await ouvrir('/actualites?categorie=8'))).toEqual(['Reportages']);
    });
  });
});
