import { Observable } from 'rxjs';
import { Reclamation } from '../models/metier.model';

/**
 * Contrat du service de reclamation.
 *
 * L'implementation actuelle utilise des donnees en memoire pour la demo.
 * Une future implementation API pourra remplacer le mock via app.config.ts
 * sans modifier les composants.
 */
export abstract class ReclamationService {
  abstract listerMesReclamations(): Observable<Reclamation[]>;
  abstract demarrerReclamation(message: string): Observable<Reclamation>;
  abstract envoyerMessage(reclamationId: string, message: string): Observable<Reclamation>;
  abstract listerPourCompagnie(): Observable<Reclamation[]>;
  abstract repondre(reclamationId: string, reponse: string, statut?: string): Observable<Reclamation>;
}
