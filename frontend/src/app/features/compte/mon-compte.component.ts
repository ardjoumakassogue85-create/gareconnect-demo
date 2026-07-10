import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';
import { AuthService } from '../../core/services/auth.service';
import { Role } from '../../core/models/auth.model';

@Component({
  selector: 'app-mon-compte',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './mon-compte.component.html',
  styleUrl: '../auth/auth.shared.scss',
})
export class MonCompteComponent implements OnInit {
  nom = '';
  email = '';
  role = signal<Role | null>(null);
  motDePasseActuel = '';
  nouveauMotDePasse = '';
  confirmationMotDePasse = '';

  chargement = signal(true);
  enregistrement = signal(false);
  message = signal<string | null>(null);
  erreur = signal<string | null>(null);

  constructor(readonly authService: AuthService) {}

  ngOnInit(): void {
    this.authService.getAccount().subscribe({
      next: (compte) => {
        this.nom = compte.nom;
        this.email = compte.email;
        this.role.set(compte.role);
        this.chargement.set(false);
      },
      error: () => {
        // Repli sur la session locale si l'appel echoue.
        const session = this.authService.currentUser();
        this.nom = session?.nom ?? '';
        this.email = session?.email ?? '';
        this.role.set(session?.role ?? null);
        this.chargement.set(false);
      },
    });
  }

  soumettre(): void {
    this.message.set(null);
    this.erreur.set(null);

    if (!this.nom.trim()) {
      this.erreur.set('Le nom est obligatoire.');
      return;
    }

    const veutChangerMotDePasse = !!this.nouveauMotDePasse || !!this.confirmationMotDePasse;
    if (veutChangerMotDePasse) {
      if (this.nouveauMotDePasse.length < 8) {
        this.erreur.set('Le nouveau mot de passe doit contenir au moins 8 caractères.');
        return;
      }
      if (this.nouveauMotDePasse !== this.confirmationMotDePasse) {
        this.erreur.set('Les deux mots de passe ne correspondent pas.');
        return;
      }
      if (!this.motDePasseActuel) {
        this.erreur.set('Renseigne ton mot de passe actuel pour le modifier.');
        return;
      }
    }

    this.enregistrement.set(true);
    this.authService
      .updateAccount({
        nom: this.nom.trim(),
        email: this.email.trim(),
        motDePasseActuel: veutChangerMotDePasse ? this.motDePasseActuel : undefined,
        nouveauMotDePasse: veutChangerMotDePasse ? this.nouveauMotDePasse : undefined,
      })
      .subscribe({
        next: () => {
          this.enregistrement.set(false);
          this.motDePasseActuel = '';
          this.nouveauMotDePasse = '';
          this.confirmationMotDePasse = '';
          this.message.set('Compte mis a jour avec succes.');
        },
        error: (err) => {
          this.enregistrement.set(false);
          this.erreur.set(err?.error?.message ?? 'Impossible de mettre a jour le compte.');
        },
      });
  }
}
