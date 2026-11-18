import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

/** Les deux saisies du nouveau mot de passe doivent être identiques. */
function confirmationIdentique(group: AbstractControl): ValidationErrors | null {
  const nouveau = group.get('nouveauMotDePasse')?.value;
  const confirmation = group.get('confirmation')?.value;
  return nouveau && confirmation && nouveau !== confirmation ? { confirmationDifferente: true } : null;
}

@Component({
  selector: 'app-mon-compte',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './mon-compte.component.html'
})
export class MonCompteComponent {
  readonly authService = inject(AuthService);
  private fb = inject(FormBuilder);

  readonly libellesRoles: Record<string, string> = {
    admin: 'Administrateur', journaliste: 'Journaliste', redacteur: 'Rédacteur', lecteur: 'Lecteur'
  };

  form = this.fb.nonNullable.group({
    motDePasseActuel: ['', Validators.required],
    nouveauMotDePasse: ['', [Validators.required, Validators.minLength(6)]],
    confirmation: ['', Validators.required]
  }, { validators: confirmationIdentique });

  loading = false;
  succes = false;
  erreur = '';

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.succes = false;
    this.erreur = '';
    const { motDePasseActuel, nouveauMotDePasse } = this.form.getRawValue();
    this.authService.changerMotDePasse(motDePasseActuel, nouveauMotDePasse).subscribe({
      next: () => {
        this.succes = true;
        this.loading = false;
        this.form.reset();
      },
      error: (err) => {
        const champs = err.error?.fieldErrors;
        this.erreur = champs ? Object.values(champs).join(' ') : (err.error?.message ?? 'Une erreur est survenue.');
        this.loading = false;
      }
    });
  }
}
