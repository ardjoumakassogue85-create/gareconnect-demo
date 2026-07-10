import { Observable } from 'rxjs';
import { DemandeVitrineCompagnie, ReservationCompagnie, Trajet, VitrineCompagnie } from '../models/metier.model';

export abstract class VitrineService {
  abstract obtenirVitrine(compagnie: string): Observable<VitrineCompagnie>;
  abstract enregistrerVitrine(demande: DemandeVitrineCompagnie): Observable<VitrineCompagnie>;
  abstract listerTrajets(compagnie: string): Observable<Trajet[]>;
  abstract listerReservations(): Observable<ReservationCompagnie[]>;
  abstract creerTrajet(compagnie: string, trajet: Omit<Trajet, 'id' | 'statut'>): Observable<Trajet>;
  abstract modifierTrajet(id: string, trajet: Omit<Trajet, 'id' | 'statut'>): Observable<Trajet>;
  abstract supprimerTrajet(id: string): Observable<void>;
  abstract basculerStatutTrajet(id: string): Observable<Trajet>;
  abstract listerTrajetsPublics(compagnie: string): Observable<Trajet[]>;
}
