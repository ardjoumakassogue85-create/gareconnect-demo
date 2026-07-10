import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AffluenceCompagnie,
  AffluenceGare,
  ContexteAffluence,
  NiveauAffluence,
} from '../models/affluence.model';

// Minutes conseillees pour arriver avant le depart selon le niveau d'affluence.
const MARGES: Record<NiveauAffluence, number> = { FAIBLE: 20, MOYENNE: 35, FORTE: 50 };

@Injectable({ providedIn: 'root' })
export class AffluenceService {
  constructor(private readonly http: HttpClient) {}

  gare(ville: string, date?: string): Observable<AffluenceGare> {
    let params = new HttpParams().set('ville', ville);
    if (date) {
      params = params.set('date', date);
    }
    return this.http
      .get<AffluenceGare>(`${environment.apiUrl}/affluence/gare`, { params })
      .pipe(timeout(8000));
  }

  compagnie(): Observable<AffluenceCompagnie> {
    return this.http
      .get<AffluenceCompagnie>(`${environment.apiUrl}/compagnies/me/affluence`)
      .pipe(timeout(8000));
  }

  contexte(ville: string, date?: string): Observable<ContexteAffluence> {
    let params = new HttpParams().set('ville', ville);
    if (date) {
      params = params.set('date', date);
    }
    // Timeout large : peut passer par l'IA. Repli neutre cote backend si indisponible.
    return this.http
      .get<ContexteAffluence>(`${environment.apiUrl}/affluence/contexte`, { params })
      .pipe(timeout(15000));
  }

  /** Heure conseillee pour arriver a la gare (depart - marge selon l'affluence). */
  heureArrivee(heureDepart: string, niveau: NiveauAffluence): string | null {
    const parts = (heureDepart ?? '').split(':');
    if (parts.length < 2) return null;
    const h = Number(parts[0]);
    const m = Number(parts[1]);
    if (Number.isNaN(h) || Number.isNaN(m)) return null;
    const minutes = Math.max(0, h * 60 + m - MARGES[niveau]);
    return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
  }
}
