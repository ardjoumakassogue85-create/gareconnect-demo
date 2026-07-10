export interface BilletToken {
  token: string;
  codeBillet: string;
}

export interface ValidationBillet {
  valide: boolean;
  message: string;
  passager: string | null;
  trajet: string | null;
  date: string | null;
  heure: string | null;
  compagnie: string | null;
  places: number;
  dejaUtilise: boolean;
  valideLe: string | null;
}

export interface ClePublique {
  cle: string;
  algorithme: string;
}

/** Resultat d'une verification hors-ligne (signature + expiration). */
export interface VerificationHorsLigne {
  authentique: boolean;
  expire: boolean;
  donnees: Record<string, unknown> | null;
}
