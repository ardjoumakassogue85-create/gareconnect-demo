import { TrajetRecherche } from './metier.model';
import { NiveauAffluence } from './affluence.model';

export interface ConseilAntiFileRequete {
  texteLibre?: string;
  villeDepart?: string;
  villeArrivee?: string;
  date?: string;
}

export interface ConseilAntiFile {
  message: string;
  resume: string;
  trajetRecommande: TrajetRecherche | null;
  heureArrivee: string | null;
  niveauAffluence: NiveauAffluence | null;
  alternatives: TrajetRecherche[];
  source: 'IA' | 'DETERMINISTE';
}

export interface CreneauArrivee {
  gare: string;
  dateVoyage: string | null;
  heureDepart: string;
  fenetreDebut: string;
  fenetreFin: string;
  position: number;
  message: string;
  dejaAttribue: boolean;
}
