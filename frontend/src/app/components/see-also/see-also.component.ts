import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Article } from '../../core/models/models';

@Component({
  selector: 'app-see-also',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './see-also.component.html'
})
export class SeeAlsoComponent {
  @Input() articles: Article[] = [];
}
