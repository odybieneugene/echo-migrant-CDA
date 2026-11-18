import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Article, IMAGE_ARTICLE_PAR_DEFAUT } from '../../core/models/models';

@Component({
  selector: 'app-featured-videos',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './featured-videos.component.html'
})
export class FeaturedVideosComponent {
  @Input() videos: Article[] = [];
  readonly imageParDefaut = IMAGE_ARTICLE_PAR_DEFAUT;
}
