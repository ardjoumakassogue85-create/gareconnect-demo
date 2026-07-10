import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';
import { CompteARceboursComponent } from '../../shared/components/compte-a-rebours/compte-a-rebours.component';
import { QrCodeComponent } from '../../shared/components/qr-code/qr-code.component';
import { AuthService } from '../../core/services/auth.service';
import { ReservationService } from '../../core/services/reservation.service';
import { GareService } from '../../core/services/gare.service';
import { BilletService } from '../../core/services/billet.service';
import { DemandeAvis, DemandeAvisGare, Reservation } from '../../core/models/metier.model';

type StatutAffiche = 'CONFIRMEE' | 'TERMINEE' | 'ANNULEE';
type FiltreOnglet = 'A_VENIR' | 'TERMINEE' | 'ANNULEE' | 'TOUTES';

@Component({
  selector: 'app-espace-client',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent, CompteARceboursComponent, QrCodeComponent],
  templateUrl: './espace-client.component.html',
  styleUrl: './espace-client.component.scss',
})
export class EspaceClientComponent implements OnInit {
  chargement = signal(true);
  reservations = signal<Reservation[]>([]);
  erreur = signal<string | null>(null);
  idEnAnnulation = signal<string | null>(null);
  filtreActif = signal<FiltreOnglet>('A_VENIR');
  reservationSelectionnee = signal<Reservation | null>(null);
  noteBrouillon = signal(0);
  commentaireBrouillon = signal('');
  envoiAvisEnCours = signal(false);
  erreurAvis = signal<string | null>(null);

  noteGareBrouillon = signal(0);
  envoiAvisGareEnCours = signal(false);
  erreurAvisGare = signal<string | null>(null);
  avisGareEnvoye = signal(false);
  moyenneGare = signal<{ moyenne: number; nombreAvis: number } | null>(null);

  readonly etoiles = [1, 2, 3, 4, 5];

  readonly onglets: { valeur: FiltreOnglet; libelle: string }[] = [
    { valeur: 'A_VENIR', libelle: 'À venir' },
    { valeur: 'TERMINEE', libelle: 'Passées' },
    { valeur: 'ANNULEE', libelle: 'Annulées' },
    { valeur: 'TOUTES', libelle: 'Toutes' },
  ];

  readonly reservationsFiltrees = computed(() => {
    const filtre = this.filtreActif();
    const toutes = this.reservations();

    if (filtre === 'TOUTES') return toutes;
    if (filtre === 'A_VENIR') return toutes.filter((r) => this.statutAffiche(r) === 'CONFIRMEE');
    return toutes.filter((r) => this.statutAffiche(r) === filtre);
  });

  billetToken = signal<string | null>(null);

  constructor(
    readonly authService: AuthService,
    private readonly reservationService: ReservationService,
    private readonly gareService: GareService,
    private readonly route: ActivatedRoute,
    private readonly billetService: BilletService,
  ) {}

  /** Contenu du QR du billet : jeton signe si disponible, sinon code lisible. */
  valeurQrBillet(reservation: Reservation): string {
    return this.billetToken() ?? reservation.codeBillet;
  }

  ngOnInit(): void {
    // Ouverture directe de la notation via une notification (?noter=<id>).
    this.charger(this.route.snapshot.queryParamMap.get('noter'));
  }

  compteurOnglet(valeur: FiltreOnglet): number {
    const toutes = this.reservations();
    if (valeur === 'TOUTES') return toutes.length;
    if (valeur === 'A_VENIR') return toutes.filter((r) => this.statutAffiche(r) === 'CONFIRMEE').length;
    return toutes.filter((r) => this.statutAffiche(r) === valeur).length;
  }

  changerFiltre(valeur: FiltreOnglet): void {
    this.filtreActif.set(valeur);
  }

  delaiAnnulationExpire(): void {
    this.reservations.update((reservations) => [...reservations]);
  }

  ouvrirDetail(r: Reservation): void {
    this.reservationSelectionnee.set(r);
    this.billetToken.set(null);
    this.billetService.token(r.id).subscribe({
      next: (billet) => this.billetToken.set(billet.token),
      error: () => this.billetToken.set(null),
    });
    this.noteBrouillon.set(r.note ?? 0);
    this.commentaireBrouillon.set(r.commentaire ?? '');
    this.erreurAvis.set(null);

    this.noteGareBrouillon.set(0);
    this.erreurAvisGare.set(null);
    this.avisGareEnvoye.set(false);
    this.moyenneGare.set(null);

    if (r.codeGareDepart) {
      this.gareService.obtenirNoteMoyenne(r.codeGareDepart).subscribe((resultat) => {
        this.moyenneGare.set({ moyenne: resultat.moyenne, nombreAvis: resultat.nombreAvis });
      });
    }
  }

  fermerDetail(): void {
    this.reservationSelectionnee.set(null);
  }

  peutNoter(r: Reservation): boolean {
    return this.statutAffiche(r) === 'TERMINEE';
  }

  choisirNote(note: number): void {
    this.noteBrouillon.set(note);
  }

  envoyerAvis(r: Reservation): void {
    const note = this.noteBrouillon();

    if (note < 1) {
      this.erreurAvis.set('Choisis une note avant d\'envoyer ton avis.');
      return;
    }

    this.erreurAvis.set(null);
    this.envoiAvisEnCours.set(true);

    const demande: DemandeAvis = {
      reservationId: r.id,
      note,
      commentaire: this.commentaireBrouillon(),
    };

    this.reservationService.laisserAvis(demande).subscribe({
      next: (reservationMiseAJour) => {
        this.envoiAvisEnCours.set(false);
        this.reservationSelectionnee.set(reservationMiseAJour);
        this.charger();
      },
      error: (err) => {
        this.envoiAvisEnCours.set(false);
        this.erreurAvis.set(err?.error?.message ?? "Impossible d'envoyer ton avis.");
      },
    });
  }

  nomGare(codeGare: string | undefined): string {
    if (!codeGare) return '';
    return this.gareService.nomLisible(codeGare);
  }

  choisirNoteGare(note: number): void {
    this.noteGareBrouillon.set(note);
  }

  envoyerAvisGare(r: Reservation): void {
    if (!r.codeGareDepart) return;
    const note = this.noteGareBrouillon();

    if (note < 1) {
      this.erreurAvisGare.set('Choisis une note avant d\'envoyer.');
      return;
    }

    this.erreurAvisGare.set(null);
    this.envoiAvisGareEnCours.set(true);

    const demande: DemandeAvisGare = { codeGare: r.codeGareDepart, note };

    this.gareService.noterGare(demande).subscribe({
      next: (resultat) => {
        this.envoiAvisGareEnCours.set(false);
        this.avisGareEnvoye.set(true);
        this.moyenneGare.set({ moyenne: resultat.moyenne, nombreAvis: resultat.nombreAvis });
      },
      error: (err) => {
        this.envoiAvisGareEnCours.set(false);
        this.erreurAvisGare.set(err?.error?.message ?? "Impossible d'envoyer ton avis.");
      },
    });
  }

  private charger(reservationANoter?: string | null): void {
    this.reservationService.listerMesReservations().subscribe((reservations) => {
      this.reservations.set(reservations);
      this.chargement.set(false);

      if (reservationANoter) {
        const cible = reservations.find((r) => r.id === reservationANoter);
        if (cible) {
          this.filtreActif.set('TERMINEE');
          this.ouvrirDetail(cible);
        }
      }
    });
  }

  statutAffiche(r: Reservation): StatutAffiche {
    if (r.statut === 'ANNULEE') return 'ANNULEE';

    const dateDepart = new Date(`${r.date}T${r.heure}`);
    if (dateDepart.getTime() < Date.now()) return 'TERMINEE';

    return 'CONFIRMEE';
  }

  libelleStatut(r: Reservation): string {
    switch (this.statutAffiche(r)) {
      case 'CONFIRMEE':
        return 'Confirmée';
      case 'TERMINEE':
        return 'Terminée';
      case 'ANNULEE':
        return 'Annulée';
    }
  }

  classeStatut(r: Reservation): string {
    return this.statutAffiche(r).toLowerCase();
  }

  estAnnulable(r: Reservation): boolean {
    return (
      this.statutAffiche(r) === 'CONFIRMEE' &&
      !!r.annulableJusquA &&
      new Date(r.annulableJusquA).getTime() > Date.now()
    );
  }

  annuler(r: Reservation): void {
    this.erreur.set(null);
    this.idEnAnnulation.set(r.id);

    this.reservationService.annulerReservation(r.id).subscribe({
      next: (reservationMiseAJour) => {
        this.idEnAnnulation.set(null);
        this.reservations.update((reservations) =>
          reservations.map((reservation) => (reservation.id === r.id ? reservationMiseAJour : reservation)),
        );
        this.fermerDetail();
        this.charger();
      },
      error: (err) => {
        this.idEnAnnulation.set(null);
        this.erreur.set(err?.error?.message ?? "Impossible d'annuler cette réservation.");
        this.charger();
      },
    });
  }
}
