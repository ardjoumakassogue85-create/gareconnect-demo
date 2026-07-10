import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DemandeAvisGare, NoteMoyenneGare } from '../models/metier.model';
import { GareService } from './gare.service';

const NOMS_GARES: Record<string, string> = {
  ABJ: "Gare d'Adjame - Abidjan",
  BYK: 'Gare centrale - Bouake',
  YAM: 'Gare de Yamoussoukro',
  SPY: 'Gare de San-Pedro',
  KHG: 'Gare de Koko · Korhogo',
};

@Injectable()
export class GareApiService implements GareService {
  constructor(private readonly http: HttpClient) {}

  noterGare(demande: DemandeAvisGare): Observable<NoteMoyenneGare> {
    return this.http
      .post<NoteMoyenneGare>(`${environment.apiUrl}/gares/${encodeURIComponent(demande.codeGare)}/avis`, demande)
      .pipe(timeout(8000));
  }

  obtenirNoteMoyenne(codeGare: string): Observable<NoteMoyenneGare> {
    return this.http
      .get<NoteMoyenneGare>(`${environment.apiUrl}/gares/${encodeURIComponent(codeGare)}/note`)
      .pipe(timeout(8000));
  }

  nomLisible(codeGare: string): string {
    return NOMS_GARES[codeGare] ?? codeGare;
  }
}
