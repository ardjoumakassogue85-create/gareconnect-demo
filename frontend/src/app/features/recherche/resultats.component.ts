import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';
import { ReservationService } from '../../core/services/reservation.service';
import { CritereRecherche, CriteresRechercheIa, TrajetRecherche } from '../../core/models/metier.model';
import { estDepartImminent, estExpire } from '../../core/utils/trajet-temps';
import { AffluenceService } from '../../core/services/affluence.service';
import { AffluenceGare, ContexteAffluence, NiveauAffluence } from '../../core/models/affluence.model';
import { AssistantService } from '../../core/services/assistant.service';
import { ConseilAntiFile, CreneauArrivee } from '../../core/models/assistant.model';

@Component({
  selector: 'app-resultats',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './resultats.component.html',
  styleUrl: './resultats.component.scss',
})
export class ResultatsComponent implements OnInit {
  chargement = signal(false);
  resultats = signal<TrajetRecherche[]>([]);
  datesAffichees = signal<string[]>([]);
  rechercheEffectuee = signal(false);
  affluence = signal<AffluenceGare | null>(null);
  contexte = signal<ContexteAffluence | null>(null);
  conseil = signal<ConseilAntiFile | null>(null);
  conseilChargement = signal(false);
  demandeAntiFile = '';
  creneau = signal<CreneauArrivee | null>(null);
  creneauChargement = signal(false);
  creneauErreur = signal<string | null>(null);

  villes = ['Abidjan', 'Bouaké', 'Yamoussoukro', 'San-Pédro', 'Korhogo'];
  villeDepart = '';
  villeArrivee = '';
  date = '';
  requeteLibre = '';
  requeteOrigine = '';
  tri: 'prix_asc' | 'prix_desc' | null = null;
  criteresIa: CriteresRechercheIa | null = null;
  messageIa: string | null = null;
  suggestionsIa = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly reservationService: ReservationService,
    private readonly affluenceService: AffluenceService,
    private readonly assistantService: AssistantService,
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    this.villeDepart = params.get('villeDepart') ?? 'Abidjan';
    this.villeArrivee = params.get('villeArrivee') ?? 'Bouaké';
    this.date = params.get('date') ?? '';
    this.requeteOrigine = params.get('requete') ?? '';
    this.requeteLibre = this.requeteOrigine;
    this.tri = params.get('tri') === 'prix_asc' ? 'prix_asc' : null;

    this.lancerRecherche();
  }

  rechercher(mettreAJourUrl = true): void {
    if (mettreAJourUrl) {
      this.requeteOrigine = '';
      this.tri = null;
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: {
          villeDepart: this.villeDepart,
          villeArrivee: this.villeArrivee,
          date: this.date || undefined,
        },
      });
    }

    this.lancerRecherche();
  }

  rechercherEnLangageLibre(): void {
    const texte = this.requeteLibre.trim();
    if (!texte) return;

    this.chargement.set(true);
    this.rechercheEffectuee.set(true);
    this.requeteOrigine = texte;

    const criteresLocaux = this.extraireCriteresLocaux(texte);
    const contexte: CritereRecherche = {
      villeDepart: criteresLocaux.villeDepart || this.villeDepart,
      villeArrivee: criteresLocaux.villeArrivee || this.villeArrivee,
      date: criteresLocaux.date || this.date || undefined,
    };

    this.reservationService.rechercherTrajetsIa(texte, contexte).subscribe({
      next: (reponse) => {
        const criteresFinaux = this.fusionnerCriteres(reponse.criteresDetectes, criteresLocaux);
        const resultatsFiltres = this.filtrerResultats(reponse.resultats, criteresFinaux, !!reponse.suggestions);

        this.criteresIa = criteresFinaux;
        this.messageIa = reponse.message || null;
        this.suggestionsIa = !!reponse.suggestions;
        this.villeDepart = criteresFinaux.villeDepart || this.villeDepart;
        this.villeArrivee = criteresFinaux.villeArrivee || this.villeArrivee;
        this.date = criteresFinaux.date || this.date;
        this.tri =
          criteresFinaux.tri === 'prix_asc' || criteresFinaux.tri === 'prix_desc'
            ? criteresFinaux.tri
            : null;

        this.resultats.set(resultatsFiltres);
        this.datesAffichees.set(
          [...new Set(resultatsFiltres.map((trajet) => trajet.date).filter((date): date is string => !!date))].sort(),
        );
        this.chargement.set(false);
        this.chargerAffluence();
        this.chargerContexte();
        this.chargerConseil(texte);

        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {
            villeDepart: this.villeDepart,
            villeArrivee: this.villeArrivee,
            date: this.date || undefined,
            tri: this.tri || undefined,
            requete: texte,
          },
        });
      },
      error: () => {
        this.criteresIa = null;
        this.messageIa = null;
        this.suggestionsIa = false;
        this.tri = null;
        this.chargement.set(false);
        this.lancerRecherche();
      },
    });
  }

  reserver(trajet: TrajetRecherche): void {
    if (trajet.placesDisponibles <= 0) return;
    this.router.navigate(['/reservation', trajet.id], {
      queryParams: { date: trajet.date || this.date || undefined },
    });
  }

  voirCompagnie(trajet: TrajetRecherche): void {
    this.router.navigate(['/vitrine', trajet.compagnie]);
  }

  private chargerAffluence(): void {
    const ville = this.villeDepart?.trim();
    if (!ville) {
      this.affluence.set(null);
      return;
    }
    this.affluenceService.gare(ville, this.date || undefined).subscribe({
      next: (a) => this.affluence.set(a),
      error: () => this.affluence.set(null),
    });
  }

  private chargerContexte(): void {
    const ville = this.villeDepart?.trim();
    if (!ville) {
      this.contexte.set(null);
      return;
    }
    this.contexte.set(null);
    this.affluenceService.contexte(ville, this.date || undefined).subscribe({
      next: (ctx) => this.contexte.set(ctx.actif ? ctx : null),
      error: () => this.contexte.set(null),
    });
  }

  private chargerConseil(texteLibre?: string): void {
    const villeDepart = this.villeDepart?.trim();
    const villeArrivee = this.villeArrivee?.trim();
    if (!villeDepart || !villeArrivee) {
      this.conseil.set(null);
      return;
    }
    this.conseilChargement.set(true);
    this.assistantService
      .conseilAntiFile({
        texteLibre: texteLibre || undefined,
        villeDepart,
        villeArrivee,
        date: this.date || undefined,
      })
      .subscribe({
        next: (conseil) => {
          this.conseil.set(conseil);
          this.conseilChargement.set(false);
          // Nouveau conseil => on remet a zero le pass coupe-file.
          this.creneau.set(null);
          this.creneauErreur.set(null);
        },
        error: () => {
          this.conseil.set(null);
          this.conseilChargement.set(false);
        },
      });
  }

  demanderConseil(): void {
    const texte = this.demandeAntiFile.trim();
    this.chargerConseil(texte || undefined);
  }

  reserverConseil(): void {
    const trajet = this.conseil()?.trajetRecommande;
    if (trajet) {
      this.reserver(trajet);
    }
  }

  prendreCreneau(): void {
    const trajet = this.conseil()?.trajetRecommande;
    if (!trajet) {
      return;
    }
    this.creneauErreur.set(null);
    this.creneauChargement.set(true);
    this.assistantService.prendreCreneauCoupeFile(trajet.id).subscribe({
      next: (creneau) => {
        this.creneau.set(creneau);
        this.creneauChargement.set(false);
      },
      error: (err) => {
        this.creneauChargement.set(false);
        this.creneauErreur.set(err?.error?.message ?? 'Impossible de réserver un créneau pour le moment.');
      },
    });
  }

  niveauTrajet(trajet: TrajetRecherche): NiveauAffluence | null {
    const a = this.affluence();
    if (!a || !trajet.heureDepart) return null;
    const heure = trajet.heureDepart.slice(0, 2) + ':00';
    return a.creneaux.find((c) => c.heure === heure)?.niveau ?? null;
  }

  heureArriveeTrajet(trajet: TrajetRecherche): string | null {
    const niveau = this.niveauTrajet(trajet);
    return niveau ? this.affluenceService.heureArrivee(trajet.heureDepart, niveau) : null;
  }

  libelleAffluence(niveau: NiveauAffluence): string {
    if (niveau === 'FORTE') return 'Forte affluence';
    if (niveau === 'MOYENNE') return 'Affluence moyenne';
    return 'Faible affluence';
  }

  niveauCourt(niveau: NiveauAffluence): string {
    return niveau.charAt(0) + niveau.slice(1).toLowerCase();
  }

  classeAffluence(niveau: NiveauAffluence | null): string {
    return niveau ? niveau.toLowerCase() : '';
  }

  libelleStatut(trajet: TrajetRecherche): string {
    if (trajet.placesDisponibles <= 0) return 'Complet';
    return estDepartImminent(trajet.date, trajet.heureDepart) ? 'Départ imminent' : "À l'heure";
  }

  classeStatut(trajet: TrajetRecherche): string {
    if (trajet.placesDisponibles <= 0) return 'complet';
    return estDepartImminent(trajet.date, trajet.heureDepart) ? 'bientot' : 'a_l_heure';
  }

  private lancerRecherche(): void {
    this.chargement.set(true);
    this.rechercheEffectuee.set(true);

    this.reservationService
      .rechercherTrajets({
        villeDepart: this.villeDepart,
        villeArrivee: this.villeArrivee,
        date: this.date,
      })
      .subscribe((resultats) => {
        const actifs = resultats.filter((trajet) => !estExpire(trajet.date, trajet.heureDepart));
        const tries = this.trierPourAffichage(actifs, { tri: this.tri });
        this.resultats.set(tries);
        this.datesAffichees.set(
          [...new Set(tries.map((trajet) => trajet.date).filter((date): date is string => !!date))].sort(),
        );
        this.chargement.set(false);
        this.chargerAffluence();
        this.chargerContexte();
        this.chargerConseil();
      });
  }

  private extraireCriteresLocaux(texte: string): CriteresRechercheIa {
    const normalise = this.normaliser(texte);
    const villeArrivee = this.villeApres(normalise, ['vers ', 'pour ', 'a ']);
    const villeDepart =
      this.villeApres(normalise, ['depuis ', 'de ', "d'"]) ??
      this.villes.find((ville) => this.normaliser(ville) !== this.normaliser(villeArrivee || '') && normalise.includes(this.normaliser(ville))) ??
      null;

    return {
      villeDepart,
      villeArrivee,
      date: this.dateTexte(normalise),
      tri: this.triTexte(normalise),
      nombreResultats: this.nombreResultatsTexte(normalise),
    };
  }

  private fusionnerCriteres(api: CriteresRechercheIa, locaux: CriteresRechercheIa): CriteresRechercheIa {
    return {
      ...api,
      villeDepart: locaux.villeDepart || api.villeDepart,
      villeArrivee: locaux.villeArrivee || api.villeArrivee,
      date: locaux.date || api.date,
      tri: locaux.tri || api.tri,
      nombreResultats: locaux.nombreResultats || api.nombreResultats,
    };
  }

  private filtrerResultats(resultats: TrajetRecherche[], criteres: CriteresRechercheIa, suggestions: boolean): TrajetRecherche[] {
    let filtres = resultats
      .filter((trajet) => !estExpire(trajet.date, trajet.heureDepart))
      .filter((trajet) => !criteres.villeDepart || this.normaliser(trajet.villeDepart) === this.normaliser(criteres.villeDepart))
      .filter((trajet) => !criteres.villeArrivee || this.normaliser(trajet.villeArrivee) === this.normaliser(criteres.villeArrivee))
      .filter((trajet) => suggestions || !criteres.date || trajet.date === criteres.date)
      .filter((trajet) => suggestions || !criteres.heureDepart || trajet.heureDepart === criteres.heureDepart)
      .filter((trajet) => suggestions || !criteres.compagnie || this.normaliser(trajet.compagnie).includes(this.normaliser(criteres.compagnie)))
      .filter((trajet) => suggestions || !criteres.prixMin || trajet.prix >= criteres.prixMin)
      .filter((trajet) => suggestions || !criteres.budgetMax || trajet.prix <= criteres.budgetMax)
      .filter((trajet) => suggestions || this.correspondAuStatut(trajet, criteres.statut));

    filtres = suggestions
      ? this.garderVoisinsDirects(this.trierPropositionsProches(filtres, criteres), criteres)
      : this.trierPourAffichage(filtres, criteres);

    const limite = !suggestions && criteres.nombreResultats && criteres.nombreResultats > 0 ? criteres.nombreResultats : undefined;
    return limite ? filtres.slice(0, limite) : filtres;
  }

  private trierPourAffichage(resultats: TrajetRecherche[], criteres: Pick<CriteresRechercheIa, 'tri'>): TrajetRecherche[] {
    const sensPrix = criteres.tri === 'prix_desc' ? -1 : 1;

    return [...resultats].sort((a, b) => {
      const parDestination = this.normaliser(a.villeArrivee).localeCompare(this.normaliser(b.villeArrivee));
      if (parDestination !== 0) return parDestination;

      const parDate = (a.date || '').localeCompare(b.date || '');
      if (parDate !== 0) return parDate;

      const parPrix = (a.prix - b.prix) * sensPrix;
      if (parPrix !== 0) return parPrix;

      const parHeure = a.heureDepart.localeCompare(b.heureDepart);
      if (parHeure !== 0) return parHeure;

      const parStatut = this.prioriteStatut(a) - this.prioriteStatut(b);
      if (parStatut !== 0) return parStatut;

      return this.normaliser(a.compagnie).localeCompare(this.normaliser(b.compagnie));
    });
  }

  private trierPropositionsProches(resultats: TrajetRecherche[], criteres: CriteresRechercheIa): TrajetRecherche[] {
    return [...resultats].sort((a, b) => {
      const parDate = this.ecartJours(a, criteres) - this.ecartJours(b, criteres);
      if (parDate !== 0) return parDate;

      const parPrix = this.ecartPrix(a, criteres) - this.ecartPrix(b, criteres);
      if (parPrix !== 0) return parPrix;

      const parHeure = this.ecartMinutes(a, criteres.heureDepart) - this.ecartMinutes(b, criteres.heureDepart);
      if (parHeure !== 0) return parHeure;

      const parStatut = this.ecartStatut(a, criteres.statut) - this.ecartStatut(b, criteres.statut);
      if (parStatut !== 0) return parStatut;

      const parCompagnie = this.ecartCompagnie(a, criteres.compagnie) - this.ecartCompagnie(b, criteres.compagnie);
      if (parCompagnie !== 0) return parCompagnie;

      return this.trierPourAffichage([a, b], criteres)[0] === a ? -1 : 1;
    });
  }

  private garderVoisinsDirects(resultats: TrajetRecherche[], criteres: CriteresRechercheIa): TrajetRecherche[] {
    if (!resultats.length) return resultats;

    let voisins = resultats;
    if (criteres.date) {
      const meilleurEcart = Math.min(...voisins.map((trajet) => this.ecartJours(trajet, criteres)));
      voisins = voisins.filter((trajet) => this.ecartJours(trajet, criteres) === meilleurEcart);
    }

    if (this.criterePrixPresent(criteres)) {
      const meilleurEcart = Math.min(...voisins.map((trajet) => this.ecartPrix(trajet, criteres)));
      voisins = voisins.filter((trajet) => this.ecartPrix(trajet, criteres) === meilleurEcart);
    }

    if (criteres.heureDepart) {
      const meilleurEcart = Math.min(...voisins.map((trajet) => this.ecartMinutes(trajet, criteres.heureDepart)));
      voisins = voisins.filter((trajet) => this.ecartMinutes(trajet, criteres.heureDepart) === meilleurEcart);
    }

    if (criteres.statut) {
      const meilleurEcart = Math.min(...voisins.map((trajet) => this.ecartStatut(trajet, criteres.statut)));
      voisins = voisins.filter((trajet) => this.ecartStatut(trajet, criteres.statut) === meilleurEcart);
    }

    if (criteres.compagnie) {
      const meilleurEcart = Math.min(...voisins.map((trajet) => this.ecartCompagnie(trajet, criteres.compagnie)));
      voisins = voisins.filter((trajet) => this.ecartCompagnie(trajet, criteres.compagnie) === meilleurEcart);
    }

    return (voisins.length ? voisins : [resultats[0]]).slice(0, 3);
  }

  private villeApres(texteNormalise: string, marqueurs: string[]): string | null {
    for (const marqueur of marqueurs) {
      const index = texteNormalise.indexOf(this.normaliser(marqueur));
      if (index === -1) continue;

      const reste = texteNormalise.slice(index + this.normaliser(marqueur).length).trim();
      const ville = this.villes.find((v) => reste.startsWith(this.normaliser(v)));
      if (ville) return ville;
    }
    return null;
  }

  private dateTexte(texteNormalise: string): string | null {
    if (texteNormalise.includes('aujourdhui')) {
      return new Date().toISOString().slice(0, 10);
    }
    if (texteNormalise.includes('demain')) {
      const demain = new Date();
      demain.setDate(demain.getDate() + 1);
      return demain.toISOString().slice(0, 10);
    }

    const mois: Record<string, string> = {
      janvier: '01',
      fevrier: '02',
      mars: '03',
      avril: '04',
      mai: '05',
      juin: '06',
      juillet: '07',
      aout: '08',
      septembre: '09',
      octobre: '10',
      novembre: '11',
      decembre: '12',
    };
    const dateTexte = texteNormalise.match(/\b(\d{1,2})\s+([a-z]+)\s+(\d{4})\b/);
    if (dateTexte && mois[dateTexte[2]]) {
      return `${dateTexte[3]}-${mois[dateTexte[2]]}-${dateTexte[1].padStart(2, '0')}`;
    }

    const dateNumerique = texteNormalise.match(/\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b/);
    if (dateNumerique) {
      return `${dateNumerique[3]}-${dateNumerique[2].padStart(2, '0')}-${dateNumerique[1].padStart(2, '0')}`;
    }

    return null;
  }

  private triTexte(texteNormalise: string): 'prix_asc' | 'prix_desc' | null {
    if (['moins cher', 'moindre cout', 'pas cher', 'economique'].some((mot) => texteNormalise.includes(mot))) {
      return 'prix_asc';
    }
    return texteNormalise.includes('plus cher') ? 'prix_desc' : null;
  }

  private nombreResultatsTexte(texteNormalise: string): number | null {
    if (['un seul', 'une seule', 'unique', 'premier resultat', 'meilleur trajet'].some((mot) => texteNormalise.includes(mot))) {
      return 1;
    }
    const nombre = texteNormalise.match(/\b([1-9])\s+(trajets|resultats|resultat)\b/);
    return nombre ? Number(nombre[1]) : null;
  }

  private correspondAuStatut(trajet: TrajetRecherche, statut: string | null | undefined): boolean {
    if (!statut) return true;
    if (statut === 'complet') return trajet.placesDisponibles <= 0;
    if (statut === 'depart_imminent') return trajet.placesDisponibles > 0 && estDepartImminent(trajet.date, trajet.heureDepart);
    if (statut === 'a_l_heure') return trajet.placesDisponibles > 0 && !estDepartImminent(trajet.date, trajet.heureDepart);
    return true;
  }

  private ecartJours(trajet: TrajetRecherche, criteres: CriteresRechercheIa): number {
    if (!criteres.date || !trajet.date) return 0;
    const demande = new Date(`${criteres.date}T00:00:00`).getTime();
    const proposition = new Date(`${trajet.date}T00:00:00`).getTime();
    if (Number.isNaN(demande) || Number.isNaN(proposition)) return 0;
    return Math.abs(proposition - demande) / 86_400_000;
  }

  private ecartPrix(trajet: TrajetRecherche, criteres: CriteresRechercheIa): number {
    if (criteres.budgetMax) return Math.abs(trajet.prix - criteres.budgetMax);
    if (criteres.prixMin) return Math.abs(trajet.prix - criteres.prixMin);
    if (criteres.tri === 'prix_desc') return -trajet.prix;
    return trajet.prix;
  }

  private ecartMinutes(trajet: TrajetRecherche, heureDemandee: string | null | undefined): number {
    if (!heureDemandee) return 0;
    return Math.abs(this.minutesDepuisMinuit(trajet.heureDepart) - this.minutesDepuisMinuit(heureDemandee));
  }

  private ecartStatut(trajet: TrajetRecherche, statut: string | null | undefined): number {
    return this.correspondAuStatut(trajet, statut) ? 0 : 1;
  }

  private ecartCompagnie(trajet: TrajetRecherche, compagnie: string | null | undefined): number {
    return !compagnie || this.normaliser(trajet.compagnie).includes(this.normaliser(compagnie)) ? 0 : 1;
  }

  private criterePrixPresent(criteres: CriteresRechercheIa): boolean {
    return !!criteres.budgetMax || !!criteres.prixMin || criteres.tri === 'prix_asc' || criteres.tri === 'prix_desc';
  }

  private minutesDepuisMinuit(heure: string): number {
    const [heures, minutes] = heure.split(':').map(Number);
    if (Number.isNaN(heures) || Number.isNaN(minutes)) return 0;
    return heures * 60 + minutes;
  }

  private prioriteStatut(trajet: TrajetRecherche): number {
    if (trajet.placesDisponibles <= 0) return 2;
    return estDepartImminent(trajet.date, trajet.heureDepart) ? 0 : 1;
  }

  private normaliser(valeur: string): string {
    return valeur
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }
}
