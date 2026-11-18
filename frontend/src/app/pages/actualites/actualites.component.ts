import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Article, NomDescription } from '../../core/models/models';
import { ArticleService } from '../../core/services/article.service';
import { ReferenceDataService } from '../../core/services/reference-data.service';

@Component({
  selector: 'app-actualites',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './actualites.component.html'
})
export class ActualitesComponent implements OnInit {
  articles: Article[] = [];
  categories: NomDescription[] = [];
  categorieFiltre: number | null = null;
  loading = true;

  constructor(
    private articleService: ArticleService,
    private referenceDataService: ReferenceDataService
  ) {}

  ngOnInit(): void {
    this.referenceDataService.getCategories().subscribe((categories) => (this.categories = categories));
    this.articleService.getPublies().subscribe({
      next: (articles) => {
        this.articles = articles;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  get articlesFiltres(): Article[] {
    if (!this.categorieFiltre) {
      return this.articles;
    }
    return this.articles.filter((a) => a.categorie?.id === this.categorieFiltre);
  }

  filtrerPar(categorieId: number | null): void {
    this.categorieFiltre = categorieId;
  }
}
