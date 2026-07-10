import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';
import { CompteARceboursComponent } from '../../shared/components/compte-a-rebours/compte-a-rebours.component';
import { QrCodeComponent } from '../../shared/components/qr-code/qr-code.component';
import { ReservationService } from '../../core/services/reservation.service';
import { BilletService } from '../../core/services/billet.service';
import { Reservation } from '../../core/models/metier.model';

@Component({
  selector: 'app-confirmation',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent, CompteARceboursComponent, QrCodeComponent],
  templateUrl: './confirmation.component.html',
  styleUrl: './confirmation.component.scss',
})
export class ConfirmationComponent implements OnInit {
  chargement = signal(true);
  reservation = signal<Reservation | null>(null);
  introuvable = signal(false);
  enAnnulation = signal(false);
  erreurAnnulation = signal<string | null>(null);
  billetToken = signal<string | null>(null);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly reservationService: ReservationService,
    private readonly billetService: BilletService,
  ) {}

  /** Contenu du QR : le jeton signe si disponible, sinon le code lisible (repli). */
  valeurQr(): string {
    return this.billetToken() ?? this.reservation()?.codeBillet ?? '';
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.introuvable.set(true);
      this.chargement.set(false);
      return;
    }

    this.charger(id);
  }

  private charger(id: string): void {
    this.reservationService.obtenirReservation(id).subscribe((reservation) => {
      this.chargement.set(false);
      if (!reservation) {
        this.introuvable.set(true);
        return;
      }
      this.reservation.set(reservation);

      // Recupere le jeton signe a encoder dans le QR (repli sur le code si echec).
      this.billetService.token(reservation.id).subscribe({
        next: (billet) => this.billetToken.set(billet.token),
        error: () => this.billetToken.set(null),
      });
    });
  }

  annuler(): void {
    const reservation = this.reservation();
    if (!reservation) return;

    this.erreurAnnulation.set(null);
    this.enAnnulation.set(true);

    this.reservationService.annulerReservation(reservation.id).subscribe({
      next: (miseAJour) => {
        this.enAnnulation.set(false);
        this.reservation.set(miseAJour);
      },
      error: (err) => {
        this.enAnnulation.set(false);
        this.erreurAnnulation.set(err?.error?.message ?? "Impossible d'annuler pour l'instant.");
        // Le délai peut avoir expiré pendant que l'utilisateur hésitait :
        // on recharge l'état réel depuis le service plutôt que de faire
        // confiance à l'état local.
        this.charger(reservation.id);
      },
    });
  }
}
