import { Observable } from 'rxjs';
import {
  CritereRecherche,
  DemandeAvis,
  DemandeReservation,
  ReponseRechercheIa,
  Reservation,
  TrajetRecherche,
} from '../models/metier.model';

/**
 * Contrat du service de réservation.
 *
 * Aujourd'hui implémenté par ReservationMockService (données en mémoire).
 * Quand le backend Phase 2 sera prêt, on créera un ReservationApiService qui
 * implémente exactement le même contrat, et on changera uniquement le
 * `provide` dans app.config.ts. Aucun composant n'a besoin d'être modifié.
 */
export abstract class ReservationService {
  abstract rechercherTrajets(criteres: CritereRecherche): Observable<TrajetRecherche[]>;
  abstract rechercherTrajetsIa(texteLibre: string, contexte?: CritereRecherche): Observable<ReponseRechercheIa>;
  abstract obtenirTrajet(id: string): Observable<TrajetRecherche | undefined>;
  abstract creerReservation(demande: DemandeReservation): Observable<Reservation>;
  abstract listerMesReservations(): Observable<Reservation[]>;
  abstract obtenirReservation(id: string): Observable<Reservation | undefined>;
  abstract annulerReservation(id: string): Observable<Reservation>;
  abstract laisserAvis(demande: DemandeAvis): Observable<Reservation>;
}
