import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ConseilAntiFile, ConseilAntiFileRequete, CreneauArrivee } from '../models/assistant.model';

@Injectable({ providedIn: 'root' })
export class AssistantService {
  constructor(private readonly http: HttpClient) {}

  conseilAntiFile(requete: ConseilAntiFileRequete): Observable<ConseilAntiFile> {
    // Timeout large : l'appel peut passer par l'IA (Gemini). Le backend retombe
    // de toute facon sur le deterministe si l'IA est lente/indisponible.
    return this.http
      .post<ConseilAntiFile>(`${environment.apiUrl}/assistant/anti-file`, requete)
      .pipe(timeout(15000));
  }

  prendreCreneauCoupeFile(trajetId: string): Observable<CreneauArrivee> {
    return this.http
      .post<CreneauArrivee>(`${environment.apiUrl}/file-virtuelle`, { trajetId: Number(trajetId) })
      .pipe(timeout(8000));
  }
}
