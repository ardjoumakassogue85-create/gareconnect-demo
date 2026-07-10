export type NiveauAffluence = 'FAIBLE' | 'MOYENNE' | 'FORTE';

export interface CreneauAffluence {
  heure: string;
  score: number;
  niveau: NiveauAffluence;
}

export interface AffluenceGare {
  gare: string;
  date: string;
  jour: string;
  niveauGlobal: NiveauAffluence;
  heureLaPlusChargee: string;
  creneauLePlusCalme: string;
  confiance: number;
  creneaux: CreneauAffluence[];
}

export interface LigneHeatmap {
  jour: string;
  creneaux: CreneauAffluence[];
}

export interface AffluenceCompagnie {
  gares: string[];
  creneaux: string[];
  heatmap: LigneHeatmap[];
  suggestions: string[];
}

export interface ContexteAffluence {
  facteur: number;
  raison: string | null;
  actif: boolean;
}
