import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Article } from '../../core/models/models';
import { ArticleService } from '../../core/services/article.service';
import { HeroSectionComponent } from '../../components/hero-section/hero-section.component';
import { RecentArticlesComponent } from '../../components/recent-articles/recent-articles.component';
import { FeaturedVideosComponent } from '../../components/featured-videos/featured-videos.component';
import { NewsletterComponent } from '../../components/newsletter/newsletter.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, HeroSectionComponent, RecentArticlesComponent, FeaturedVideosComponent, NewsletterComponent],
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  articles: Article[] = [];
  loading = true;

  constructor(private articleService: ArticleService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.articleService.getPublies(9).subscribe({
      next: (articles) => {
        this.articles = articles;
        this.loading = false;
        this.allerAlAncre();
      },
      error: () => {
        this.loading = false;
        this.allerAlAncre();
      }
    });
  }

  /**
   * Arrivée par « S'abonner » (/#newsletter) depuis une autre page : la section n'existe qu'une fois
   * les articles chargés, donc le défilement automatique du routeur ne la trouve pas. On le fait ici.
   */
  private allerAlAncre(): void {
    const ancre = this.route.snapshot.fragment;
    if (ancre) {
      setTimeout(() => document.getElementById(ancre)?.scrollIntoView({ behavior: 'smooth' }));
    }
  }

  get articleVedette(): Article | undefined {
    return this.articles.find((a) => a.estEnVedette) ?? this.articles[0];
  }

  get autresArticles(): Article[] {
    const vedette = this.articleVedette;
    return this.articles.filter((a) => a.id !== vedette?.id);
  }

  get videos(): Article[] {
    return this.articles.filter((a) => a.format?.nom === 'video' || a.format?.nom === 'podcast');
  }
}
