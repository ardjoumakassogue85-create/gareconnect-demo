import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Reclamation } from '../models/metier.model';
import { ReclamationService } from './reclamation.service';

@Injectable()
export class ReclamationApiService implements ReclamationService {
  constructor(private readonly http: HttpClient) {}

  listerMesReclamations(): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(`${environment.apiUrl}/reclamations/me`).pipe(timeout(8000));
  }

  demarrerReclamation(message: string): Observable<Reclamation> {
    return this.http.post<Reclamation>(`${environment.apiUrl}/reclamations`, { message }).pipe(timeout(8000));
  }

  envoyerMessage(reclamationId: string, message: string): Observable<Reclamation> {
    return this.http
      .post<Reclamation>(`${environment.apiUrl}/reclamations/${reclamationId}/messages`, { message })
      .pipe(timeout(8000));
  }
  listerPourCompagnie(): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(`${environment.apiUrl}/compagnies/me/reclamations`).pipe(timeout(20000));
  }

  repondre(reclamationId: string, reponse: string, statut?: string): Observable<Reclamation> {
    return this.http
      .patch<Reclamation>(`${environment.apiUrl}/compagnies/me/reclamations/${reclamationId}`, { reponse, statut })
      .pipe(timeout(8000));
  }
}
