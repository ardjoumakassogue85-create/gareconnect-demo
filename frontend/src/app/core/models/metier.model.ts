// Modeles metier de demonstration.
// Ils seront remplaces/completes par les DTOs reels une fois le backend metier disponible.

export type StatutReservation = 'CONFIRMEE' | 'EN_ATTENTE' | 'ANNULEE';
export type StatutPaiement = 'PAYE' | 'REMBOURSE' | 'ECHEC';
export type MethodePaiement = 'MOBILE_MONEY' | 'CARTE';
export type OperateurMobileMoney = 'WAVE' | 'ORANGE_MONEY' | 'MTN_MONEY' | 'MOOV_MONEY';

export interface CritereRecherche {
  villeDepart?: string;
  villeArrivee?: string;
  date?: string;
}

export interface CriteresRechercheIa {
  villeDepart?: string | null;
  villeArrivee?: string | null;
  date?: string | null;
  budgetMax?: number | null;
  tri?: 'prix_asc' | 'prix_desc' | string | null;
  nombreResultats?: number | null;
  heureDepart?: string | null;
  compagnie?: string | null;
  prixMin?: number | null;
  statut?: string | null;
}

export interface ReponseRechercheIa {
  criteresDetectes: CriteresRechercheIa;
  resultats: TrajetRecherche[];
  message?: string | null;
  suggestions?: boolean;
}

export interface TrajetRecherche {
  id: string;
  codeGareDepart: string;
  codeGareArrivee: string;
  villeDepart: string;
  villeArrivee: string;
  date?: string;
  compagnie: string;
  heureDepart: string;
  duree: string;
  prix: number;
  placesDisponibles: number;
}

export interface DemandeReservation {
  trajetId: string;
  methodePaiement: MethodePaiement;
  dateVoyage?: string;
  nombreTickets?: number;
  operateurMobileMoney?: OperateurMobileMoney;
}

export interface Reservation {
  id: string;
  trajetId?: string;
  codeBillet: string;
  codeGareDepart?: string;
  codeGareArrivee?: string;
  villeDepart: string;
  villeArrivee: string;
  date: string;
  heure: string;
  compagnie: string;
  prix: number;
  nombreTickets?: number;
  statut: StatutReservation;
  statutPaiement?: StatutPaiement;
  methodePaiement?: MethodePaiement;
  operateurMobileMoney?: OperateurMobileMoney;
  creeLe?: string;
  annulableJusquA: string | null;
  note?: number | null;
  commentaire?: string | null;
}

export interface ReservationCompagnie {
  id: string;
  client: string;
  trajet: string;
  date?: string | null;
  tickets: number;
  total: number;
  statut: 'CONFIRMEE' | 'ANNULEE';
  paiement: 'PAYE' | 'REMBOURSE' | 'ECHEC';
}

export interface DemandeAvis {
  reservationId: string;
  note: number;
  commentaire?: string;
}

export interface DemandeAvisGare {
  codeGare: string;
  note: number;
  commentaire?: string;
}

export interface NoteMoyenneGare {
  codeGare: string;
  moyenne: number;
  nombreAvis: number;
}

export type AuteurMessage = 'CLIENT' | 'ASSISTANT' | 'ADMIN';
export type StatutReclamation = 'REPONDUE_IA' | 'EN_ATTENTE_ADMIN' | 'RESOLUE_ADMIN';

export interface MessageReclamation {
  id: string;
  auteur: AuteurMessage;
  texte: string;
  envoyeLe: string;
}

export interface Reclamation {
  id: string;
  client: string;
  sujet: string;
  statut: StatutReclamation;
  messages: MessageReclamation[];
  creeLe: string;
  majLe: string;
}

export type StatutTrajet = 'ACTIF' | 'SUSPENDU';

export interface VitrineCompagnie {
  compagnie: string;
  slug: string;
  description: string;
  logoUrl?: string | null;
  imageCouvertureUrl?: string | null;
  galerieImages: string[];
  localisation?: string;
  noteMoyenne: number;
  nombreAvis: number;
  garesDesservies: string[];
  flotte: string[];
}

export interface DemandeVitrineCompagnie {
  compagnie: string;
  description: string;
  logoUrl?: string | null;
  imageCouvertureUrl?: string | null;
  galerieImages?: string[];
  localisation?: string;
  garesDesservies?: string[];
  flotte?: string[];
}

export interface Trajet {
  id: string;
  compagnie?: string;
  codeGareDepart?: string;
  codeGareArrivee?: string;
  villeDepart: string;
  villeArrivee: string;
  date?: string;
  heureDepart: string;
  prix: number;
  placesDisponibles: number;
  statut: StatutTrajet;
}
