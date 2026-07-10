import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LayoutComponent } from '../../../shared/components/layout/layout.component';
import { AuthService } from '../../../core/services/auth.service';
import { Role } from '../../../core/models/auth.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './register.component.html',
  styleUrl: '../auth.shared.scss',
})
export class RegisterComponent {
  role: Role = 'CLIENT';
  nom = '';
  email = '';
  password = '';
  confirmationPassword = '';
  codeVerification = '';
  emailEnVerification = '';
  verificationDemandee = signal(false);
  message = signal<string | null>(null);
  erreur = signal<string | null>(null);
  chargement = signal(false);
  verificationEnCours = signal(false);
  renvoiEnCours = signal(false);
  motDePasseVisible = signal(false);
  confirmationVisible = signal(false);

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    route: ActivatedRoute,
  ) {
    const roleDemande = route.snapshot.queryParamMap.get('role');
    if (roleDemande === 'COMPAGNIE') {
      this.role = 'COMPAGNIE';
    }

    const email = route.snapshot.queryParamMap.get('email');
    const code = route.snapshot.queryParamMap.get('code');
    if (email) {
      this.email = email;
      this.emailEnVerification = email;
      this.verificationDemandee.set(true);
    }
    if (code) {
      this.codeVerification = code;
    }
  }

  choisirRole(role: Role): void {
    this.role = role;
  }

  basculerVisibiliteMotDePasse(): void {
    this.motDePasseVisible.update((visible) => !visible);
  }

  basculerVisibiliteConfirmation(): void {
    this.confirmationVisible.update((visible) => !visible);
  }

  soumettre(): void {
    this.erreur.set(null);
    this.message.set(null);

    if (this.password !== this.confirmationPassword) {
      this.erreur.set('Les deux mots de passe ne correspondent pas.');
      return;
    }

    this.chargement.set(true);

    this.authService
      .register({ email: this.email, password: this.password, nom: this.nom, role: this.role })
      .subscribe({
        next: (reponse) => {
          this.chargement.set(false);
          this.emailEnVerification = reponse.email;
          this.verificationDemandee.set(true);
          this.message.set(reponse.message);
        },
        error: (err) => {
          this.chargement.set(false);
          this.erreur.set(err?.error?.message ?? "Impossible de creer le compte pour l'instant.");
        },
      });
  }

  verifierEmail(): void {
    this.erreur.set(null);
    this.message.set(null);

    const email = this.emailEnVerification || this.email;
    if (!email || !this.codeVerification.trim()) {
      this.erreur.set('Renseigne le code recu par email.');
      return;
    }

    this.verificationEnCours.set(true);
    this.authService.verifyEmail({ email, code: this.codeVerification.trim() }).subscribe({
      next: () => {
        this.router.navigate(['/connexion'], { queryParams: { verified: '1', email } });
      },
      error: (err) => {
        this.verificationEnCours.set(false);
        this.erreur.set(err?.error?.message ?? 'Impossible de verifier ce code.');
      },
    });
  }

  renvoyerCode(): void {
    this.erreur.set(null);
    this.message.set(null);

    const email = this.emailEnVerification || this.email;
    if (!email) {
      this.erreur.set("Renseigne l'email du compte a verifier.");
      return;
    }

    this.renvoiEnCours.set(true);
    this.authService.resendVerification(email).subscribe({
      next: (reponse) => {
        this.renvoiEnCours.set(false);
        this.emailEnVerification = reponse.email;
        this.message.set(reponse.message);
      },
      error: (err) => {
        this.renvoiEnCours.set(false);
        this.erreur.set(err?.error?.message ?? 'Impossible de renvoyer le code pour le moment.');
      },
    });
  }
}
