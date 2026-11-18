import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Article } from '../../core/models/models';

@Component({
  selector: 'app-featured-videos',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './featured-videos.component.html'
})
export class FeaturedVideosComponent {
  @Input() videos: Article[] = [];
}
