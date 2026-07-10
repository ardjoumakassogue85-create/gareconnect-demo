import { Observable } from 'rxjs';
import { DemandeAvisGare, NoteMoyenneGare } from '../models/metier.model';

/**
 * Contrat du service de notation des gares.
 *
 * Distinct de ReservationService.laisserAvis (qui note le trajet/la compagnie) :
 * ici on note l'expérience à la gare elle-même — accueil, propreté, ponctualité
 * de l'embarquement — indépendamment de la compagnie qui opère le trajet.
 *
 * Aujourd'hui implémenté par GareMockService (données en mémoire, partagées
 * entre tous les utilisateurs de l'onglet le temps de la session).
 */
export abstract class GareService {
  abstract noterGare(demande: DemandeAvisGare): Observable<NoteMoyenneGare>;
  abstract obtenirNoteMoyenne(codeGare: string): Observable<NoteMoyenneGare>;
  abstract nomLisible(codeGare: string): string;
}
