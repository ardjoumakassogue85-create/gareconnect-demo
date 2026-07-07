import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CritereRecherche,
  DemandeAvis,
  DemandeReservation,
  ReponseRechercheIa,
  Reservation,
  TrajetRecherche,
} from '../models/metier.model';
import { ReservationService } from './reservation.service';

@Injectable()
export class ReservationApiService implements ReservationService {
  constructor(private readonly http: HttpClient) {}

  rechercherTrajets(criteres: CritereRecherche): Observable<TrajetRecherche[]> {
    let params = new HttpParams();
    if (criteres.villeDepart) params = params.set('villeDepart', criteres.villeDepart);
    if (criteres.villeArrivee) params = params.set('villeArrivee', criteres.villeArrivee);
    if (criteres.date) params = params.set('date', criteres.date);

    return this.http
      .get<TrajetRecherche[]>(`${environment.apiUrl}/trajets/recherche`, { params })
      .pipe(timeout(8000));
  }

  rechercherTrajetsIa(texteLibre: string, contexte?: CritereRecherche): Observable<ReponseRechercheIa> {
    return this.http
      .post<ReponseRechercheIa>(`${environment.apiUrl}/trajets/recherche-ia`, { texteLibre, ...contexte })
      .pipe(timeout(15000));
  }

  obtenirTrajet(id: string): Observable<TrajetRecherche | undefined> {
    return this.http.get<TrajetRecherche>(`${environment.apiUrl}/trajets/${id}`).pipe(timeout(8000));
  }

  creerReservation(demande: DemandeReservation): Observable<Reservation> {
    return this.http.post<Reservation>(`${environment.apiUrl}/reservations`, demande).pipe(timeout(8000));
  }

  listerMesReservations(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${environment.apiUrl}/reservations/me`).pipe(timeout(8000));
  }

  obtenirReservation(id: string): Observable<Reservation | undefined> {
    return this.http.get<Reservation>(`${environment.apiUrl}/reservations/${id}`).pipe(timeout(8000));
  }

  annulerReservation(id: string): Observable<Reservation> {
    return this.http.patch<Reservation>(`${environment.apiUrl}/reservations/${id}/annuler`, {}).pipe(timeout(8000));
  }

  laisserAvis(demande: DemandeAvis): Observable<Reservation> {
    return this.http
      .post<Reservation>(`${environment.apiUrl}/reservations/${demande.reservationId}/avis`, demande)
      .pipe(timeout(8000));
  }
}
