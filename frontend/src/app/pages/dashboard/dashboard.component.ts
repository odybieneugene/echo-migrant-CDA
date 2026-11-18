import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Article, ArticleRequest } from '../../core/models/models';
import { ArticleService } from '../../core/services/article.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardArticleFormComponent } from '../../components/dashboard-article-form/dashboard-article-form.component';
import { DashboardUtilisateursComponent } from '../../components/dashboard-utilisateurs/dashboard-utilisateurs.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DashboardArticleFormComponent, DashboardUtilisateursComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  onglet: 'articles' | 'utilisateurs' = 'articles';
  articles: Article[] = [];
  loading = true;
  erreur = '';
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

  supprimerArticle(article: Article): void {
    if (!confirm(`Supprimer l'article "${article.titre}" ?`)) {
      return;
    }
    this.articleService.delete(article.id).subscribe(() => this.chargerArticles());
  }

  onSave(event: { data: ArticleRequest; image: File | null }): void {
    const request$ = this.articleEnEdition
      ? this.articleService.update(this.articleEnEdition.id, event.data, event.image)
      : this.articleService.create(event.data, event.image);

    request$.subscribe(() => {
      this.showForm = false;
      this.chargerArticles();
    });
  }

  onCancel(): void {
    this.showForm = false;
  }
}
