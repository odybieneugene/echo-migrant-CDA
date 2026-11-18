import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-newsletter',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './newsletter.component.html'
})
export class NewsletterComponent {
  email = '';
  submitted = false;

  subscribe(): void {
    // Pas d'endpoint newsletter dans le MLD actuel : simple confirmation visuelle côté client.
    this.submitted = !!this.email;
  }
}
