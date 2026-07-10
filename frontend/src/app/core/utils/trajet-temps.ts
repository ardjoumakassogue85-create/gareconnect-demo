// Utilitaires de temps pour les trajets (statut "Depart imminent", trajets expires).
// Regle metier : "Depart imminent" = il reste 30 minutes ou moins avant le depart.

export const SEUIL_DEPART_IMMINENT_MINUTES = 30;

/**
 * Minutes restantes avant le depart (peut etre negatif si deja parti).
 * Retourne null si la date ou l'heure sont absentes/illisibles.
 */
export function minutesAvantDepart(date?: string | null, heure?: string | null): number | null {
  if (!date || !heure) {
    return null;
  }
  const heureNormalisee = heure.length === 5 ? `${heure}:00` : heure;
  const depart = new Date(`${date}T${heureNormalisee}`);
  if (Number.isNaN(depart.getTime())) {
    return null;
  }
  return (depart.getTime() - Date.now()) / 60_000;
}

/** Vrai si le depart est deja passe. */
export function estExpire(date?: string | null, heure?: string | null): boolean {
  const minutes = minutesAvantDepart(date, heure);
  return minutes !== null && minutes < 0;
}

/** Vrai s'il reste 30 minutes ou moins (et que le depart n'est pas encore passe). */
export function estDepartImminent(date?: string | null, heure?: string | null): boolean {
  const minutes = minutesAvantDepart(date, heure);
  return minutes !== null && minutes >= 0 && minutes <= SEUIL_DEPART_IMMINENT_MINUTES;
}
