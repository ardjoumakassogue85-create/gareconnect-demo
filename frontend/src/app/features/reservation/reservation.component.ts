import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';
import { ReservationService } from '../../core/services/reservation.service';
import { AuthService } from '../../core/services/auth.service';
import {
  MethodePaiement,
  OperateurMobileMoney,
  TrajetRecherche,
} from '../../core/models/metier.model';

interface ChoixOperateur {
  valeur: OperateurMobileMoney;
  libelle: string;
}

@Component({
  selector: 'app-reservation',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './reservation.component.html',
  styleUrl: './reservation.component.scss',
})
export class ReservationComponent implements OnInit {
  chargement = signal(true);
  trajet = signal<TrajetRecherche | null>(null);
  introuvable = signal(false);

  methode: MethodePaiement = 'MOBILE_MONEY';
  nombreTickets = 1;
  operateurMobileMoney: OperateurMobileMoney = 'WAVE';
  numeroMobileMoney = '';
  numeroCarte = '';
  expirationCarte = '';
  dateVoyage = '';
  readonly operateursMobileMoney: ChoixOperateur[] = [
    { valeur: 'WAVE', libelle: 'Wave' },
    { valeur: 'ORANGE_MONEY', libelle: 'Orange Money' },
    { valeur: 'MTN_MONEY', libelle: 'MTN Money' },
    { valeur: 'MOOV_MONEY', libelle: 'Moov Money' },
  ];

  enTraitement = signal(false);
  erreur = signal<string | null>(null);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly reservationService: ReservationService,
    readonly authService: AuthService,
  ) {}

  ngOnInit(): void {
    const trajetId = this.route.snapshot.paramMap.get('trajetId');
    const dateDemandee = this.route.snapshot.queryParamMap.get('date');
    if (!trajetId) {
      this.introuvable.set(true);
      this.chargement.set(false);
      return;
    }

    this.reservationService.obtenirTrajet(trajetId).subscribe((trajet) => {
      this.chargement.set(false);
      if (!trajet) {
        this.introuvable.set(true);
        return;
      }
      this.dateVoyage = trajet.date || dateDemandee || new Date().toISOString().slice(0, 10);
      this.trajet.set({ ...trajet, date: this.dateVoyage });
    });
  }

  choisirMethode(methode: MethodePaiement): void {
    this.methode = methode;
    this.erreur.set(null);
  }

  choisirOperateur(operateur: OperateurMobileMoney): void {
    this.operateurMobileMoney = operateur;
    this.erreur.set(null);
  }

  totalAPayer(trajet: TrajetRecherche): number {
    return trajet.prix * this.nombreTicketsValide();
  }

  private nombreTicketsValide(): number {
    return Math.max(1, Math.floor(Number(this.nombreTickets) || 1));
  }

  confirmerEtPayer(): void {
    const trajet = this.trajet();
    if (!trajet) return;

    this.erreur.set(null);
    this.nombreTickets = this.nombreTicketsValide();

    if (this.nombreTickets > trajet.placesDisponibles) {
      this.erreur.set(`Il reste seulement ${trajet.placesDisponibles} place(s) disponible(s).`);
      return;
    }

    this.enTraitement.set(true);

    this.reservationService
      .creerReservation({
        trajetId: trajet.id,
        methodePaiement: this.methode,
        dateVoyage: trajet.date || this.dateVoyage,
        nombreTickets: this.nombreTickets,
        operateurMobileMoney:
          this.methode === 'MOBILE_MONEY' ? this.operateurMobileMoney : undefined,
      })
      .subscribe({
        next: (reservation) => {
          this.router.navigate(['/confirmation', reservation.id]);
        },
        error: (err) => {
          this.enTraitement.set(false);
          this.erreur.set(err?.error?.message ?? 'Le paiement a échoué. Réessaie.');
        },
      });
  }
}
