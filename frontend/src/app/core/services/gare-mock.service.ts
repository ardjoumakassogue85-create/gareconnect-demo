import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { DemandeAvisGare, NoteMoyenneGare } from '../models/metier.model';
import { GareService } from './gare.service';

const LATENCE_SIMULEE_MS = 300;

const NOMS_GARES: Record<string, string> = {
  ABJ: "Gare d'Adjamé · Abidjan",
  BYK: 'Gare centrale · Bouaké',
  YAM: 'Gare de Yamoussoukro',
  SPY: 'Gare de San-Pédro',
  KHG: 'Gare de Koko · Korhogo',
};

interface AvisGareEnregistre {
  codeGare: string;
  note: number;
  commentaire?: string;
}

// Quelques avis de démo pour que les moyennes ne partent pas de zéro à l'écran.
const AVIS_INITIAUX: AvisGareEnregistre[] = [
  { codeGare: 'ABJ', note: 4 },
  { codeGare: 'ABJ', note: 5 },
  { codeGare: 'ABJ', note: 4 },
  { codeGare: 'BYK', note: 3 },
  { codeGare: 'BYK', note: 4 },
  { codeGare: 'YAM', note: 5 },
];

@Injectable()
export class GareMockService implements GareService {
  private avis: AvisGareEnregistre[] = [...AVIS_INITIAUX];

  nomLisible(codeGare: string): string {
    return NOMS_GARES[codeGare] ?? codeGare;
  }

  noterGare(demande: DemandeAvisGare): Observable<NoteMoyenneGare> {
    if (demande.note < 1 || demande.note > 5) {
      return throwError(() => ({ error: { message: 'La note doit être comprise entre 1 et 5.' } }));
    }

    this.avis.push({
      codeGare: demande.codeGare,
      note: demande.note,
      commentaire: demande.commentaire?.trim() || undefined,
    });

    return this.calculerMoyenne(demande.codeGare).pipe(delay(LATENCE_SIMULEE_MS));
  }

  obtenirNoteMoyenne(codeGare: string): Observable<NoteMoyenneGare> {
    return this.calculerMoyenne(codeGare).pipe(delay(LATENCE_SIMULEE_MS));
  }

  private calculerMoyenne(codeGare: string): Observable<NoteMoyenneGare> {
    const avisGare = this.avis.filter((a) => a.codeGare === codeGare);
    const moyenne = avisGare.length
      ? avisGare.reduce((somme, a) => somme + a.note, 0) / avisGare.length
      : 0;

    return of({
      codeGare,
      moyenne: Math.round(moyenne * 10) / 10,
      nombreAvis: avisGare.length,
    });
  }
}
