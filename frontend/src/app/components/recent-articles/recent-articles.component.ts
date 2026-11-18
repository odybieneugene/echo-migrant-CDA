import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Article } from '../../core/models/models';
import { FeaturedArticleComponent } from '../featured-article/featured-article.component';

@Component({
  selector: 'app-recent-articles',
  standalone: true,
  imports: [CommonModule, FeaturedArticleComponent],
  templateUrl: './recent-articles.component.html'
})
export class RecentArticlesComponent {
  @Input({ required: true }) articles: Article[] = [];
}
