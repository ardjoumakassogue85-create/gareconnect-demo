import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LayoutComponent } from '../../../shared/components/layout/layout.component';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './reset-password.component.html',
  styleUrl: '../auth.shared.scss',
})
export class ResetPasswordComponent implements OnInit {
  token = '';
  password = '';
  confirmationPassword = '';
  erreur = signal<string | null>(null);
  succes = signal(false);
  chargement = signal(false);

  constructor(
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.erreur.set('Lien de reinitialisation invalide. Refais une demande depuis la page de connexion.');
    }
  }

  soumettre(): void {
    this.erreur.set(null);

    if (!this.token) {
      this.erreur.set('Lien de reinitialisation invalide. Refais une demande depuis la page de connexion.');
      return;
    }

    if (this.password.length < 8) {
      this.erreur.set('Le mot de passe doit contenir au moins 8 caractères.');
      return;
    }

    if (this.password !== this.confirmationPassword) {
      this.erreur.set('Les deux mots de passe ne correspondent pas.');
      return;
    }

    this.chargement.set(true);
    this.authService.resetPassword({ token: this.token, password: this.password }).subscribe({
      next: () => {
        this.chargement.set(false);
        this.succes.set(true);
        setTimeout(() => this.router.navigate(['/connexion'], { queryParams: { reset: '1' } }), 1500);
      },
      error: (err) => {
        this.chargement.set(false);
        this.erreur.set(err?.error?.message ?? 'Impossible de reinitialiser le mot de passe.');
      },
    });
  }
}
