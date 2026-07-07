import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { DemandeVitrineCompagnie, ReservationCompagnie, Trajet, VitrineCompagnie } from '../models/metier.model';
import { VitrineService } from './vitrine.service';

const LATENCE_SIMULEE_MS = 250;

const normaliser = (valeur: string): string =>
  valeur
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');

const VITRINES_INITIALES: VitrineCompagnie[] = [
  {
    compagnie: 'UTB',
    slug: 'utb',
    description: 'Compagnie interurbaine reliant Abidjan, Bouake et Korhogo avec des departs quotidiens.',
    logoUrl: null,
    imageCouvertureUrl: null,
    galerieImages: [],
    localisation: "Gare d'Adjame, Abidjan",
    noteMoyenne: 4.4,
    nombreAvis: 128,
    garesDesservies: ["Gare d'Adjame", 'Bouake centre', 'Korhogo Koko'],
    flotte: ['Cars climatises', 'Bagages etiquetes', 'Depart controle'],
  },
  {
    compagnie: 'STIF',
    slug: 'stif',
    description: 'Reseau rapide entre Abidjan et Yamoussoukro, pense pour les voyageurs reguliers.',
    logoUrl: null,
    imageCouvertureUrl: null,
    galerieImages: [],
    localisation: 'Yamoussoukro',
    noteMoyenne: 4.1,
    nombreAvis: 86,
    garesDesservies: ["Gare d'Adjame", 'Yamoussoukro'],
    flotte: ['Cars confort', 'Paiement mobile', 'Service client'],
  },
  {
    compagnie: 'CTM',
    slug: 'ctm',
    description: 'Dessertes longues distances vers San-Pedro et les grandes villes du pays.',
    logoUrl: null,
    imageCouvertureUrl: null,
    galerieImages: [],
    localisation: 'San-Pedro',
    noteMoyenne: 4,
    nombreAvis: 73,
    garesDesservies: ["Gare d'Adjame", 'San-Pedro'],
    flotte: ['Soutes securisees', 'Sieges inclinables', 'Assistance embarquement'],
  },
];

const TRAJETS_INITIAUX: Trajet[] = [
  {
    id: 'v-utb-1',
    compagnie: 'UTB',
    codeGareDepart: 'ABJ',
    codeGareArrivee: 'BYK',
    villeDepart: 'Abidjan',
    villeArrivee: 'Bouake',
    date: '2026-07-15',
    heureDepart: '06:30',
    prix: 6000,
    placesDisponibles: 18,
    statut: 'ACTIF',
  },
  {
    id: 'v-utb-2',
    compagnie: 'UTB',
    codeGareDepart: 'ABJ',
    codeGareArrivee: 'KHG',
    villeDepart: 'Abidjan',
    villeArrivee: 'Korhogo',
    date: '2026-07-16',
    heureDepart: '09:45',
    prix: 9000,
    placesDisponibles: 0,
    statut: 'SUSPENDU',
  },
  {
    id: 'v-stif-1',
    compagnie: 'STIF',
    codeGareDepart: 'YAM',
    codeGareArrivee: 'ABJ',
    villeDepart: 'Yamoussoukro',
    villeArrivee: 'Abidjan',
    date: '2026-07-15',
    heureDepart: '08:00',
    prix: 4500,
    placesDisponibles: 11,
    statut: 'ACTIF',
  },
];

@Injectable()
export class VitrineMockService implements VitrineService {
  private vitrines = VITRINES_INITIALES.map((v) => ({ ...v }));
  private trajets = TRAJETS_INITIAUX.map((t) => ({ ...t }));

  obtenirVitrine(compagnie: string): Observable<VitrineCompagnie> {
    const vitrine = this.trouverOuCreerVitrine(compagnie);
    return of({
      ...vitrine,
      galerieImages: [...vitrine.galerieImages],
      localisation: vitrine.localisation,
      garesDesservies: [...vitrine.garesDesservies],
      flotte: [...vitrine.flotte],
    }).pipe(delay(LATENCE_SIMULEE_MS));
  }

  enregistrerVitrine(demande: DemandeVitrineCompagnie): Observable<VitrineCompagnie> {
    const compagnie = demande.compagnie.trim() || 'Compagnie';
    const slug = normaliser(compagnie) || 'compagnie';
    const existante = this.trouverOuCreerVitrine(compagnie);

    existante.compagnie = compagnie;
    existante.slug = slug;
    existante.description = demande.description.trim();
    existante.logoUrl = demande.logoUrl ?? null;
    existante.imageCouvertureUrl = demande.imageCouvertureUrl ?? null;
    existante.galerieImages = [...(demande.galerieImages ?? [])];
    existante.localisation = demande.localisation?.trim() ?? existante.localisation ?? '';
    existante.garesDesservies = [...(demande.garesDesservies ?? existante.garesDesservies)];
    existante.flotte = [...(demande.flotte ?? existante.flotte)];

    return this.obtenirVitrine(compagnie);
  }

  listerTrajets(compagnie: string): Observable<Trajet[]> {
    const slug = normaliser(compagnie);
    const resultats = this.trajets
      .filter((trajet) => normaliser(trajet.compagnie ?? '') === slug)
      .map((trajet) => ({ ...trajet }));

    return of(resultats).pipe(delay(LATENCE_SIMULEE_MS));
  }

  listerReservations(): Observable<ReservationCompagnie[]> {
    return of([]).pipe(delay(LATENCE_SIMULEE_MS));
  }

  creerTrajet(compagnie: string, trajet: Omit<Trajet, 'id' | 'statut'>): Observable<Trajet> {
    const nouveau: Trajet = {
      ...trajet,
      id: crypto.randomUUID(),
      compagnie,
      statut: 'ACTIF',
    };

    this.trajets.unshift(nouveau);
    return of({ ...nouveau }).pipe(delay(LATENCE_SIMULEE_MS));
  }

  modifierTrajet(id: string, trajet: Omit<Trajet, 'id' | 'statut'>): Observable<Trajet> {
    const index = this.trajets.findIndex((t) => t.id === id);
    if (index < 0) {
      return throwError(() => ({ error: { message: 'Trajet introuvable.' } }));
    }

    this.trajets[index] = {
      ...this.trajets[index],
      ...trajet,
      compagnie: trajet.compagnie ?? this.trajets[index].compagnie,
    };

    return of({ ...this.trajets[index] }).pipe(delay(LATENCE_SIMULEE_MS));
  }

  supprimerTrajet(id: string): Observable<void> {
    this.trajets = this.trajets.filter((trajet) => trajet.id !== id);
    return of(void 0).pipe(delay(LATENCE_SIMULEE_MS));
  }

  basculerStatutTrajet(id: string): Observable<Trajet> {
    const trajet = this.trajets.find((t) => t.id === id);
    if (!trajet) {
      return throwError(() => ({ error: { message: 'Trajet introuvable.' } }));
    }

    trajet.statut = trajet.statut === 'ACTIF' ? 'SUSPENDU' : 'ACTIF';
    return of({ ...trajet }).pipe(delay(LATENCE_SIMULEE_MS));
  }

  private trouverOuCreerVitrine(compagnie: string): VitrineCompagnie {
    const nom = compagnie.trim() || 'Compagnie';
    const slug = normaliser(nom) || 'compagnie';
    let vitrine = this.vitrines.find((v) => v.slug === slug || normaliser(v.compagnie) === slug);

    if (!vitrine) {
      vitrine = {
        compagnie: nom,
        slug,
        description: 'Compagnie de transport interurbain reliant les voyageurs aux principales gares.',
        logoUrl: null,
        imageCouvertureUrl: null,
        galerieImages: [],
        localisation: '',
        noteMoyenne: 0,
        nombreAvis: 0,
        garesDesservies: [],
        flotte: [],
      };
      this.vitrines.push(vitrine);
    }

    return vitrine;
  }
}
