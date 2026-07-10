import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LayoutComponent } from '../../../shared/components/layout/layout.component';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './login.component.html',
  styleUrl: '../auth.shared.scss',
})
export class LoginComponent implements OnInit {
  email = '';
  password = '';
  rememberMe = false;
  erreur = signal<string | null>(null);
  message = signal<string | null>(null);
  chargement = signal(false);
  envoiMotDePasse = signal(false);
  motDePasseVisible = signal(false);

  constructor(
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('retour') === 'recherche') {
      this.message.set('Connecte-toi pour continuer ta recherche. Tes informations sont conservees.');
    }

    if (this.route.snapshot.queryParamMap.get('verified') === '1') {
      this.message.set('Email verifie avec succes. Tu peux maintenant te connecter.');
      this.email = this.route.snapshot.queryParamMap.get('email') ?? this.email;
    }

    if (this.route.snapshot.queryParamMap.get('reset') === '1') {
      this.message.set('Mot de passe reinitialise. Connecte-toi avec ton nouveau mot de passe.');
    }
  }

  basculerVisibiliteMotDePasse(): void {
    this.motDePasseVisible.update((visible) => !visible);
  }

  soumettre(): void {
    this.erreur.set(null);
    this.chargement.set(true);

    this.authService.login({ email: this.email, password: this.password }, this.rememberMe).subscribe({
      next: (reponse) => {
        const recherche = this.authService.consommerRechercheEnAttente();
        if (reponse.role === 'CLIENT' && recherche) {
          this.router.navigate(['/recherche'], { queryParams: recherche });
          return;
        }

        this.authService.allerAuDashboard(reponse.role);
      },
      error: (err) => {
        this.chargement.set(false);
        this.erreur.set(err?.error?.message ?? 'Connexion impossible. Verifie tes identifiants.');
      },
    });
  }

  motDePasseOublie(): void {
    this.erreur.set(null);
    this.message.set(null);

    const email = this.email.trim();
    if (!email) {
      this.erreur.set("Renseigne ton email pour recevoir le lien de reinitialisation.");
      return;
    }

    this.envoiMotDePasse.set(true);
    this.authService.forgotPassword({ email }).subscribe({
      next: (reponse) => {
        this.envoiMotDePasse.set(false);
        this.message.set(reponse.message);
      },
      error: (err) => {
        this.envoiMotDePasse.set(false);
        this.erreur.set(err?.error?.message ?? 'Impossible de traiter la demande pour le moment.');
      },
    });
  }
}
