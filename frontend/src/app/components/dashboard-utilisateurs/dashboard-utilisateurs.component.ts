import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Utilisateur } from '../../core/models/models';
import { UtilisateurService } from '../../core/services/utilisateur.service';
import { AuthService } from '../../core/services/auth.service';

/** Rôles fixes posés par les migrations, du plus élevé au plus simple. */
export const ROLES = [
  { nom: 'admin', libelle: 'Administrateur' },
  { nom: 'journaliste', libelle: 'Journaliste' },
  { nom: 'redacteur', libelle: 'Rédacteur' },
  { nom: 'lecteur', libelle: 'Lecteur' }
];

/**
 * Onglet "Utilisateurs" du dashboard (admin) : attribuer un rôle, supprimer un compte.
 * Un compte = un rôle dans cette interface (l'API accepte plusieurs rôles).
 */
@Component({
  selector: 'app-dashboard-utilisateurs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-utilisateurs.component.html'
})
export class DashboardUtilisateursComponent implements OnInit {
  readonly roles = ROLES;
  utilisateurs: Utilisateur[] = [];
  loading = true;
  message: { type: 'success' | 'danger'; texte: string } | null = null;

  constructor(private utilisateurService: UtilisateurService, private authService: AuthService) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.utilisateurService.getAll().subscribe({
      next: (utilisateurs) => {
        this.utilisateurs = utilisateurs;
        this.loading = false;
      },
      error: (err) => {
        this.afficherErreur(err);
        this.loading = false;
      }
    });
  }

  /** Rôle principal affiché : le plus élevé que possède l'utilisateur. */
  rolePrincipal(utilisateur: Utilisateur): string {
    return ROLES.find((r) => utilisateur.roles.includes(r.nom))?.nom ?? 'lecteur';
  }

  estMoi(utilisateur: Utilisateur): boolean {
    return utilisateur.id === this.authService.currentUser()?.id;
  }

  changerRole(utilisateur: Utilisateur, nouveauRole: string): void {
    this.utilisateurService.updateRoles(utilisateur.id, [nouveauRole]).subscribe({
      next: (maj) => {
        utilisateur.roles = maj.roles;
        const libelle = ROLES.find((r) => r.nom === nouveauRole)?.libelle ?? nouveauRole;
        this.message = { type: 'success', texte: `${utilisateur.prenom} ${utilisateur.nom} est maintenant : ${libelle}.` };
      },
      error: (err) => {
        this.afficherErreur(err);
        this.charger(); // remet la liste déroulante sur la valeur réelle
      }
    });
  }

  supprimer(utilisateur: Utilisateur): void {
    if (!confirm(`Supprimer définitivement le compte de ${utilisateur.prenom} ${utilisateur.nom} (${utilisateur.email}) ?`)) {
      return;
    }
    this.utilisateurService.delete(utilisateur.id).subscribe({
      next: () => {
        this.utilisateurs = this.utilisateurs.filter((u) => u.id !== utilisateur.id);
        this.message = { type: 'success', texte: `Compte de ${utilisateur.prenom} ${utilisateur.nom} supprimé.` };
      },
      error: (err) => this.afficherErreur(err)
    });
  }

  private afficherErreur(err: HttpErrorResponse): void {
    this.message = { type: 'danger', texte: err.error?.message ?? 'Une erreur est survenue.' };
  }
}
