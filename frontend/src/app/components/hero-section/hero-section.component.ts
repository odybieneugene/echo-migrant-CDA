import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Article, IMAGE_ARTICLE_PAR_DEFAUT } from '../../core/models/models';

@Component({
  selector: 'app-hero-section',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './hero-section.component.html'
})
export class HeroSectionComponent {
  readonly imageParDefaut = IMAGE_ARTICLE_PAR_DEFAUT;
  @Input({ required: true }) article!: Article;
}
