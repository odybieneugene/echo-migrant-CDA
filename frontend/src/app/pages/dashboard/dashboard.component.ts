import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Article, ArticleRequest } from '../../core/models/models';
import { ArticleService } from '../../core/services/article.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardArticleFormComponent } from '../../components/dashboard-article-form/dashboard-article-form.component';
import { DashboardUtilisateursComponent } from '../../components/dashboard-utilisateurs/dashboard-utilisateurs.component';
import { DashboardNewsletterComponent } from '../../components/dashboard-newsletter/dashboard-newsletter.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DashboardArticleFormComponent, DashboardUtilisateursComponent, DashboardNewsletterComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  onglet: 'articles' | 'utilisateurs' | 'newsletter' = 'articles';
  articles: Article[] = [];
  loading = true;
  erreur = '';
  /** Erreur d'enregistrement ou de suppression renvoyée par l'API. */
  erreurAction = '';
  showForm = false;
  articleEnEdition: Article | null = null;

  constructor(private articleService: ArticleService, public authService: AuthService) {}

  ngOnInit(): void {
    this.chargerArticles();
  }

  chargerArticles(): void {
    this.loading = true;
    this.erreur = '';
    this.articleService.getAllAdmin().subscribe({
      next: (articles) => {
        this.articles = articles;
        this.loading = false;
      },
      error: (err) => {
        // Sans ce message, une erreur s'affichait comme "Aucun article pour le moment"
        this.erreur = err.error?.message ?? 'Impossible de charger les articles.';
        this.loading = false;
      }
    });
  }

  nouvelArticle(): void {
    this.articleEnEdition = null;
    this.showForm = true;
  }

  modifierArticle(article: Article): void {
    this.articleEnEdition = article;
    this.showForm = true;
  }

  /** Mêmes règles que l'API : seuls admin et journaliste suppriment. */
  peutSupprimer(): boolean {
    return this.authService.hasAnyRole(['admin', 'journaliste']);
  }

  /** Mêmes règles que l'API : admin et journaliste modifient tout, un rédacteur seulement ses articles. */
  peutModifier(article: Article): boolean {
    return this.peutSupprimer() || article.auteurId === this.authService.currentUser()?.id;
  }

  supprimerArticle(article: Article): void {
    if (!confirm(`Supprimer l'article "${article.titre}" ?`)) {
      return;
    }
    this.erreurAction = '';
    this.articleService.delete(article.id).subscribe({
      next: () => this.chargerArticles(),
      error: (err) => (this.erreurAction = this.messageErreur(err, 'La suppression a échoué.'))
    });
  }

  onSave(event: { data: ArticleRequest; image: File | null }): void {
    const request$ = this.articleEnEdition
      ? this.articleService.update(this.articleEnEdition.id, event.data, event.image)
      : this.articleService.create(event.data, event.image);

    this.erreurAction = '';
    request$.subscribe({
      next: () => {
        this.showForm = false;
        this.chargerArticles();
      },
      // Le formulaire reste ouvert : la saisie n'est pas perdue
      error: (err) => (this.erreurAction = this.messageErreur(err, "L'enregistrement a échoué."))
    });
  }

  onCancel(): void {
    this.showForm = false;
    this.erreurAction = '';
  }

  /** Message de l'API (ou détail des champs invalides), sinon message par défaut. */
  private messageErreur(err: { error?: { message?: string; fieldErrors?: Record<string, string> } }, parDefaut: string): string {
    const champs = err.error?.fieldErrors;
    return champs ? Object.values(champs).join(' ') : (err.error?.message ?? parDefaut);
  }
}
