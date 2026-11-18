import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { NewsletterService } from '../../core/services/newsletter.service';

@Component({
  selector: 'app-newsletter',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './newsletter.component.html'
})
export class NewsletterComponent {
  email = '';
  submitted = false;
  loading = false;
  erreur = '';

  constructor(private newsletterService: NewsletterService) {}

  subscribe(form: NgForm): void {
    if (form.invalid) {
      this.erreur = 'Veuillez saisir une adresse email valide.';
      return;
    }
    this.loading = true;
    this.erreur = '';
    this.newsletterService.inscrire(this.email).subscribe({
      next: () => {
        this.submitted = true;
        this.loading = false;
      },
      error: (err) => {
        this.erreur = err.error?.fieldErrors?.email ?? err.error?.message ?? "L'inscription a échoué, réessayez plus tard.";
        this.loading = false;
      }
    });
  }
}
