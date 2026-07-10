import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import {
  CritereRecherche,
  DemandeAvis,
  DemandeReservation,
  ReponseRechercheIa,
  Reservation,
  TrajetRecherche,
} from '../models/metier.model';
import { ReservationService } from './reservation.service';

const DUREE_ANNULATION_MS = 30 * 60 * 1000; // 30 minutes
const LATENCE_SIMULEE_MS = 300; // pour se rapprocher d'un vrai appel réseau

// Catalogue de démonstration — remplacé par la table `trajets` en Phase 2
const CATALOGUE_DEMO: TrajetRecherche[] = [
  {
    id: 't-abj-byk-0630',
    codeGareDepart: 'ABJ',
    codeGareArrivee: 'BYK',
    villeDepart: 'Abidjan',
    date: '2026-07-06',
    villeArrivee: 'Bouaké',
    compagnie: 'UTB',
    heureDepart: '06:30',
    duree: '4h30',
    prix: 6000,
    placesDisponibles: 12,
  },
  {
    id: 't-abj-byk-1507-0630',
    codeGareDepart: 'ABJ',
    codeGareArrivee: 'BYK',
    villeDepart: 'Abidjan',
    date: '2026-07-15',
    villeArrivee: 'BouakÃ©',
    compagnie: 'UTB',
    heureDepart: '06:30',
    duree: '4h30',
    prix: 6000,
    placesDisponibles: 18,
  },
  {
    id: 't-abj-byk-1507-1030',
    codeGareDepart: 'ABJ',
    codeGareArrivee: 'BYK',
    villeDepart: 'Abidjan',
    date: '2026-07-15',
    villeArrivee: 'BouakÃ©',
    compagnie: 'STIF',
    heureDepart: '10:30',
    duree: '4h45',
    prix: 5500,
    placesDisponibles: 7,
  },
  {
    id: 't-abj-byk-1607-1430',
    codeGareDepart: 'ABJ',
    codeGareArrivee: 'BYK',
    villeDepart: 'Abidjan',
    date: '2026-07-16',
    villeArrivee: 'BouakÃ©',
    compagnie: 'CTM',
    heureDepart: '14:30',
    duree: '4h40',
    prix: 6500,
    placesDisponibles: 4,
  },
  {
    id: 't-abj-yam-0715',
    codeGareDepart: 'ABJ',
    codeGareArrivee: 'YAM',
    villeDepart: 'Abidjan',
    date: '2026-07-07',
    villeArrivee: 'Yamoussoukro',
    compagnie: 'STIF',
    heureDepart: '07:15',
    duree: '3h00',
    prix: 4500,
    placesDisponibles: 8,
  },
  {
    id: 't-yam-abj-1507-0800',
    codeGareDepart: 'YAM',
    codeGareArrivee: 'ABJ',
    villeDepart: 'Yamoussoukro',
    date: '2026-07-15',
    villeArrivee: 'Abidjan',
    compagnie: 'STIF',
    heureDepart: '08:00',
    duree: '3h00',
    prix: 4500,
    placesDisponibles: 11,
  },
  {
    id: 't-abj-spy-0800',
    codeGareDepart: 'ABJ',
    codeGareArrivee: 'SPY',
    villeDepart: 'Abidjan',
    date: '2026-07-08',
    villeArrivee: 'San-Pédro',
    compagnie: 'CTM',
    heureDepart: '08:00',
    duree: '6h00',
    prix: 7500,
    placesDisponibles: 5,
  },
  {
    id: 't-byk-khg-1507-0900',
    codeGareDepart: 'BYK',
    codeGareArrivee: 'KHG',
    villeDepart: 'BouakÃ©',
    date: '2026-07-15',
    villeArrivee: 'Korhogo',
    compagnie: 'UTB',
    heureDepart: '09:00',
    duree: '4h00',
    prix: 5500,
    placesDisponibles: 20,
  },
  {
    id: 't-spy-abj-1807-0700',
    codeGareDepart: 'SPY',
    codeGareArrivee: 'ABJ',
    villeDepart: 'San-PÃ©dro',
    date: '2026-07-18',
    villeArrivee: 'Abidjan',
    compagnie: 'CTM',
    heureDepart: '07:00',
    duree: '6h00',
    prix: 7500,
    placesDisponibles: 9,
  },
  {
    id: 't-abj-khg-0945',
    codeGareDepart: 'ABJ',
    codeGareArrivee: 'KHG',
    villeDepart: 'Abidjan',
    date: '2026-07-09',
    villeArrivee: 'Korhogo',
    compagnie: 'UTB',
    heureDepart: '09:45',
    duree: '8h30',
    prix: 9000,
    placesDisponibles: 0,
  },
];

@Injectable()
export class ReservationMockService implements ReservationService {
  private readonly trajets: TrajetRecherche[] = CATALOGUE_DEMO.map((t) => ({ ...t }));
  private readonly reservations: Reservation[] = [];

  rechercherTrajets(criteres: CritereRecherche): Observable<TrajetRecherche[]> {
    const resultats = this.trajets
      .filter((t) => {
        const matchDepart = !criteres.villeDepart || t.villeDepart === criteres.villeDepart;
        const matchArrivee = !criteres.villeArrivee || t.villeArrivee === criteres.villeArrivee;
        const matchDate = !criteres.date || t.date === criteres.date;
        return matchDepart && matchArrivee && matchDate;
      })
      .map((t) => ({ ...t }));
    return of(resultats).pipe(delay(LATENCE_SIMULEE_MS));
  }

  rechercherTrajetsIa(texteLibre: string, contexte?: CritereRecherche): Observable<ReponseRechercheIa> {
    const texteNormalise = texteLibre
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
    const resultats = this.trajets
      .filter((t) => !contexte?.villeDepart || t.villeDepart === contexte.villeDepart)
      .filter((t) => !contexte?.villeArrivee || t.villeArrivee === contexte.villeArrivee)
      .filter((t) => !contexte?.date || t.date === contexte.date)
      .sort((a, b) => a.prix - b.prix);
    const limite = texteNormalise.includes('un seul') ? 1 : resultats.length;
    return of({
      criteresDetectes: {
        tri: 'prix_asc',
        nombreResultats: limite === 1 ? 1 : null,
      },
      resultats: resultats.slice(0, limite),
    }).pipe(delay(LATENCE_SIMULEE_MS));
  }

  obtenirTrajet(id: string): Observable<TrajetRecherche | undefined> {
    return of(this.trajets.find((t) => t.id === id)).pipe(delay(LATENCE_SIMULEE_MS));
  }

  creerReservation(demande: DemandeReservation): Observable<Reservation> {
    const trajet = this.trajets.find((t) => t.id === demande.trajetId);

    if (!trajet) {
      return throwError(() => ({ error: { message: 'Trajet introuvable.' } }));
    }

    const nombreTickets = Math.max(1, Math.floor(Number(demande.nombreTickets) || 1));

    if (trajet.placesDisponibles <= 0) {
      return throwError(() => ({ error: { message: 'Ce trajet est complet.' } }));
    }

    if (nombreTickets > trajet.placesDisponibles) {
      return throwError(() => ({
        error: { message: `Il reste seulement ${trajet.placesDisponibles} place(s) disponible(s).` },
      }));
    }

    trajet.placesDisponibles -= nombreTickets;

    const maintenant = new Date();
    const reservation: Reservation = {
      id: crypto.randomUUID(),
      trajetId: trajet.id,
      codeBillet: 'RG-' + Math.floor(10000 + Math.random() * 89999),
      codeGareDepart: trajet.codeGareDepart,
      codeGareArrivee: trajet.codeGareArrivee,
      villeDepart: trajet.villeDepart,
      villeArrivee: trajet.villeArrivee,
      date: demande.dateVoyage || maintenant.toISOString().slice(0, 10),
      heure: trajet.heureDepart,
      compagnie: trajet.compagnie,
      prix: trajet.prix * nombreTickets,
      nombreTickets,
      statut: 'CONFIRMEE',
      statutPaiement: 'PAYE',
      methodePaiement: demande.methodePaiement,
      operateurMobileMoney: demande.operateurMobileMoney,
      creeLe: maintenant.toISOString(),
      annulableJusquA: new Date(maintenant.getTime() + DUREE_ANNULATION_MS).toISOString(),
    };

    this.reservations.unshift(reservation);
    return of(reservation).pipe(delay(LATENCE_SIMULEE_MS));
  }

  listerMesReservations(): Observable<Reservation[]> {
    return of([...this.reservations]).pipe(delay(LATENCE_SIMULEE_MS));
  }

  obtenirReservation(id: string): Observable<Reservation | undefined> {
    return of(this.reservations.find((r) => r.id === id)).pipe(delay(LATENCE_SIMULEE_MS));
  }

  annulerReservation(id: string): Observable<Reservation> {
    const reservation = this.reservations.find((r) => r.id === id);

    if (!reservation) {
      return throwError(() => ({ error: { message: 'Réservation introuvable.' } }));
    }

    if (reservation.statut === 'ANNULEE') {
      return throwError(() => ({ error: { message: 'Cette réservation est déjà annulée.' } }));
    }

    // Vérification côté "serveur" (même en mock, on ne fait jamais confiance
    // au chrono du navigateur pour la décision réelle).
    const dansLesTemps =
      reservation.annulableJusquA && new Date() <= new Date(reservation.annulableJusquA);

    if (!dansLesTemps) {
      return throwError(() => ({
        error: { message: "Le délai d'annulation de 30 minutes est dépassé." },
      }));
    }

    reservation.statut = 'ANNULEE';
    reservation.statutPaiement = 'REMBOURSE';
    reservation.annulableJusquA = null;

    const trajet = this.trajets.find((t) => t.id === reservation.trajetId);
    if (trajet) {
      trajet.placesDisponibles += reservation.nombreTickets ?? 1;
    }

    return of(reservation).pipe(delay(LATENCE_SIMULEE_MS));
  }

  laisserAvis(demande: DemandeAvis): Observable<Reservation> {
    const reservation = this.reservations.find((r) => r.id === demande.reservationId);

    if (!reservation) {
      return throwError(() => ({ error: { message: 'Réservation introuvable.' } }));
    }

    if (demande.note < 1 || demande.note > 5) {
      return throwError(() => ({ error: { message: 'La note doit être comprise entre 1 et 5.' } }));
    }

    reservation.note = demande.note;
    reservation.commentaire = demande.commentaire?.trim() || null;

    return of(reservation).pipe(delay(LATENCE_SIMULEE_MS));
  }
}
