import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DemandeVitrineCompagnie, ReservationCompagnie, Trajet, VitrineCompagnie } from '../models/metier.model';
import { VitrineService } from './vitrine.service';

@Injectable()
export class VitrineApiService implements VitrineService {
  constructor(private readonly http: HttpClient) {}

  obtenirVitrine(compagnie: string): Observable<VitrineCompagnie> {
    return this.http
      .get<VitrineCompagnie>(`${environment.apiUrl}/vitrines/${encodeURIComponent(compagnie)}`)
      .pipe(timeout(8000));
  }

  enregistrerVitrine(demande: DemandeVitrineCompagnie): Observable<VitrineCompagnie> {
    return this.http
      .put<VitrineCompagnie>(`${environment.apiUrl}/compagnies/me/vitrine`, demande)
      .pipe(timeout(8000));
  }

  listerTrajets(): Observable<Trajet[]> {
    return this.http.get<Trajet[]>(`${environment.apiUrl}/compagnies/me/trajets`).pipe(timeout(8000));
  }
 listerTrajetsPublics(compagnie: string): Observable<Trajet[]> {
    return this.http
      .get<Trajet[]>(`${environment.apiUrl}/vitrines/${encodeURIComponent(compagnie)}/trajets`)
      .pipe(timeout(8000));
  }
  listerReservations(): Observable<ReservationCompagnie[]> {
    return this.http
      .get<ReservationCompagnie[]>(`${environment.apiUrl}/compagnies/me/reservations`)
      .pipe(timeout(8000));
  }

  creerTrajet(_compagnie: string, trajet: Omit<Trajet, 'id' | 'statut'>): Observable<Trajet> {
    return this.http.post<Trajet>(`${environment.apiUrl}/compagnies/me/trajets`, trajet).pipe(timeout(8000));
  }

  modifierTrajet(id: string, trajet: Omit<Trajet, 'id' | 'statut'>): Observable<Trajet> {
    return this.http.put<Trajet>(`${environment.apiUrl}/compagnies/me/trajets/${id}`, trajet).pipe(timeout(8000));
  }

  supprimerTrajet(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/compagnies/me/trajets/${id}`).pipe(timeout(8000));
  }

  basculerStatutTrajet(id: string): Observable<Trajet> {
    return this.http.patch<Trajet>(`${environment.apiUrl}/compagnies/me/trajets/${id}/statut`, {}).pipe(timeout(8000));
  }
}
