import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Article, Commentaire } from '../../core/models/models';
import { ArticleService } from '../../core/services/article.service';
import { CommentaireService } from '../../core/services/commentaire.service';
import { AuthService } from '../../core/services/auth.service';
import { SeeAlsoComponent } from '../../components/see-also/see-also.component';

@Component({
  selector: 'app-article-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, SeeAlsoComponent],
  templateUrl: './article-detail.component.html'
})
export class ArticleDetailComponent implements OnInit {
  article: Article | null = null;
  autresArticles: Article[] = [];
  commentaires: Commentaire[] = [];
  nouveauCommentaire = '';
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private articleService: ArticleService,
    private commentaireService: CommentaireService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      const id = Number(params['id']);
      this.chargerArticle(id);
    });
  }

  private chargerArticle(id: number): void {
    this.loading = true;
    this.articleService.getById(id).subscribe({
      next: (article) => {
        this.article = article;
        this.loading = false;
        this.chargerCommentaires(id);
        this.chargerSuggestions();
      },
      error: () => (this.loading = false)
    });
  }

  private chargerCommentaires(articleId: number): void {
    this.commentaireService.getByArticle(articleId).subscribe((commentaires) => (this.commentaires = commentaires));
  }

  private chargerSuggestions(): void {
    this.articleService.getPublies(4).subscribe((articles) => {
      this.autresArticles = articles.filter((a) => a.id !== this.article?.id).slice(0, 3);
    });
  }

  posterCommentaire(): void {
    if (!this.article || !this.nouveauCommentaire.trim()) {
      return;
    }
    this.commentaireService.create(this.article.id, this.nouveauCommentaire).subscribe((commentaire) => {
      this.commentaires = [commentaire, ...this.commentaires];
      this.nouveauCommentaire = '';
    });
  }
}
