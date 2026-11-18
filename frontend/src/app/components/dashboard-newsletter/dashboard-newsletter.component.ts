import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InscriptionNewsletter } from '../../core/models/models';
import { NewsletterService } from '../../core/services/newsletter.service';

/** Onglet "Newsletter" du dashboard (admin) : liste des inscrits, désinscription (ex. demande RGPD). */
@Component({
  selector: 'app-dashboard-newsletter',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-newsletter.component.html'
})
export class DashboardNewsletterComponent implements OnInit {
  inscriptions: InscriptionNewsletter[] = [];
  loading = true;
  erreur = '';

  constructor(private newsletterService: NewsletterService) {}

  ngOnInit(): void {
    this.newsletterService.getAll().subscribe({
      next: (inscriptions) => {
        this.inscriptions = inscriptions;
        this.loading = false;
      },
      error: (err) => {
        this.erreur = err.error?.message ?? 'Impossible de charger les inscriptions.';
        this.loading = false;
      }
    });
  }

  desinscrire(inscription: InscriptionNewsletter): void {
    if (!confirm(`Désinscrire ${inscription.email} de la newsletter ?`)) {
      return;
    }
    this.newsletterService.delete(inscription.id).subscribe({
      next: () => (this.inscriptions = this.inscriptions.filter((i) => i.id !== inscription.id)),
      error: (err) => (this.erreur = err.error?.message ?? 'La désinscription a échoué.')
    });
  }
}
