import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';
import { AuthService, RechercheEnAttente } from '../../core/services/auth.service';
import { ReservationService } from '../../core/services/reservation.service';
import { GareService } from '../../core/services/gare.service';
import { TrajetRecherche } from '../../core/models/metier.model';

interface VitrineGare {
  code: string;
  compagnie: string;
  gare: string;
  trajets: { ligne: string; prix: string }[];
  places: string;
}

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
})
export class LandingComponent implements OnInit, OnDestroy {
  villes = ['Abidjan', 'Bouak\u00e9', 'Yamoussoukro', 'San-P\u00e9dro', 'Korhogo'];

  villeDepart = 'Abidjan';
  villeArrivee = 'Bouak\u00e9';
  date = '';
  requeteLibre = '';

  vitrinesGares: VitrineGare[] = [];

  vitrineActive = 0;
  private vitrineTimer?: ReturnType<typeof setInterval>;

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly reservationService: ReservationService,
    private readonly gareService: GareService,
  ) {}

  ngOnInit(): void {
    this.reservationService.rechercherTrajets({}).subscribe((trajets) => {
      this.vitrinesGares = this.construireVitrines(trajets);
      this.vitrineActive = 0;
    });
    this.vitrineTimer = setInterval(() => this.vitrineSuivante(), 3500);
  }

  ngOnDestroy(): void {
    if (this.vitrineTimer) {
      clearInterval(this.vitrineTimer);
    }
  }

  choisirVitrine(index: number): void {
    this.vitrineActive = index;
  }

  rechercher(): void {
    this.allerVersRecherche({
      villeDepart: this.villeDepart,
      villeArrivee: this.villeArrivee,
      date: this.date || undefined,
    });
  }

  /**
   * Assistant IA en langage libre.
   *
   * MVP hackathon : heuristique locale (regex sur les noms de ville connus,
   * "aujourd'hui"/"demain", et un signal de petit budget). Aucun appel réseau,
   * donc ça marche même sans connexion pendant la démo.
   *
   * Phase suivante : remplacer le corps de cette méthode par un appel à
   * l'endpoint IA (ex. POST /api/assistant/interpreter), qui renverra les
   * mêmes champs { villeDepart, villeArrivee, date, tri }. La navigation vers
   * /recherche ne change pas.
   */
  rechercherEnLangageLibre(): void {
    const texte = this.requeteLibre.trim();
    if (!texte) return;

    const texteNormalise = texte
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    const villeTrouvee = (motsCles: string[]): string | undefined => {
      for (const mot of motsCles) {
        const index = texteNormalise.indexOf(mot);
        if (index === -1) continue;
        const reste = texteNormalise.slice(index + mot.length).trim();
        const ville = this.villes.find((v) => {
          const vNormalise = v
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '');
          return reste.startsWith(vNormalise);
        });
        if (ville) return ville;
      }
      return undefined;
    };

    const villeArrivee = villeTrouvee(['vers ', 'pour ', 'a ']);
    const villeDepart =
      villeTrouvee(['depuis ', 'de ']) ??
      this.villes.find((v) => v !== villeArrivee && texteNormalise.includes(v.toLowerCase()));

    let date = '';
    const aujourdHui = new Date();
    if (texteNormalise.includes("aujourd'hui") || texteNormalise.includes('aujourdhui')) {
      date = aujourdHui.toISOString().slice(0, 10);
    } else if (texteNormalise.includes('demain')) {
      const demain = new Date(aujourdHui);
      demain.setDate(demain.getDate() + 1);
      date = demain.toISOString().slice(0, 10);
    }

    const petitBudget = ['pas trop cher', 'petit budget', 'economique', 'moins cher'].some((m) =>
      texteNormalise.includes(m),
    );

    this.allerVersRecherche({
      villeDepart: villeDepart ?? this.villeDepart,
      villeArrivee: villeArrivee ?? this.villeArrivee,
      date: date || undefined,
      tri: petitBudget ? 'prix_asc' : undefined,
      requete: texte,
    });
  }

  private allerVersRecherche(recherche: RechercheEnAttente): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/recherche'], { queryParams: recherche });
      return;
    }

    this.authService.memoriserRechercheEnAttente(recherche);
    this.router.navigate(['/connexion'], {
      queryParams: { retour: 'recherche' },
    });
  }

  private vitrineSuivante(): void {
    this.vitrineActive = (this.vitrineActive + 1) % this.vitrinesGares.length;
  }

  private construireVitrines(trajets: TrajetRecherche[]): VitrineGare[] {
    const parCompagnie = new Map<string, TrajetRecherche[]>();
    for (const trajet of trajets) {
      const nom = trajet.compagnie?.trim() || 'Compagnie';
      const liste = parCompagnie.get(nom) ?? [];
      liste.push(trajet);
      parCompagnie.set(nom, liste);
    }

    return Array.from(parCompagnie.entries()).map(([compagnie, trajetsCompagnie]) => {
      const trajetAleatoire = trajetsCompagnie[Math.floor(Math.random() * trajetsCompagnie.length)];
      return {
        code: compagnie.charAt(0).toUpperCase(),
        compagnie,
        gare: this.gareService.nomLisible(trajetAleatoire.codeGareDepart),
        trajets: [
          {
            ligne: `${trajetAleatoire.villeDepart} -> ${trajetAleatoire.villeArrivee}`,
            prix: `${trajetAleatoire.prix.toLocaleString('fr-FR')} F`,
          },
        ],
        places: `${trajetAleatoire.placesDisponibles} dispo.`,
      };
    });
  }
}