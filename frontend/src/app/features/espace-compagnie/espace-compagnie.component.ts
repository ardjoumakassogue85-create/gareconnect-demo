import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';
import { AuthService } from '../../core/services/auth.service';
import { ReservationCompagnie, Trajet, VitrineCompagnie } from '../../core/models/metier.model';
import { VitrineService } from '../../core/services/vitrine.service';

interface ReclamationCompagnie {
  id: string;
  client: string;
  sujet: string;
  trajet: string;
  priorite: 'Haute' | 'Normale';
  statut: 'EN_ATTENTE' | 'REPONDUE' | 'RESOLUE';
  message: string;
  reponse: string;
}

@Component({
  selector: 'app-espace-compagnie',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './espace-compagnie.component.html',
  styleUrl: './espace-compagnie.component.scss',
})
export class EspaceCompagnieComponent implements OnInit {
  constructor(
    readonly authService: AuthService,
    private readonly vitrineService: VitrineService,
  ) {}

  description = 'Compagnie de transport interurbain reliant Abidjan aux principales villes du pays.';
  localisation = '';
  garesDesserviesTexte = '';
  servicesTexte = '';
  logoApercu = signal<string | null>(null);
  imageCouverture = signal<string | null>(null);
  galerieImages = signal<string[]>([]);
  vitrine = signal<VitrineCompagnie | null>(null);
  trajets = signal<Trajet[]>([]);
  rechercheTrajet = signal('');
  rechercheReservation = signal('');
  villes = ['Abidjan', 'Bouaké', 'Yamoussoukro', 'San-Pédro', 'Korhogo'];

  reservations = signal<ReservationCompagnie[]>([]);

  reclamations = signal<ReclamationCompagnie[]>([
    {
      id: 'rec-107',
      client: 'Awa Coulibaly',
      sujet: 'Bagage signale perdu',
      trajet: 'Abidjan -> Bouake',
      priorite: 'Haute',
      statut: 'EN_ATTENTE',
      message: "L'assistant IA n'a pas pu confirmer le suivi du bagage. La cliente demande un retour humain.",
      reponse: '',
    },
    {
      id: 'rec-108',
      client: 'Serge Bamba',
      sujet: 'Retard au depart',
      trajet: 'Abidjan -> Korhogo',
      priorite: 'Normale',
      statut: 'REPONDUE',
      message: 'Le client demande une explication sur le retard du depart de 09:45.',
      reponse: 'Bonjour, le depart a ete retarde par le controle technique. Merci pour votre patience.',
    },
  ]);

  formulaireOuvert = signal(false);
  formulaireVitrineOuvert = signal(false);
  tousLesTrajetsOuverts = signal(false);
  reclamationSelectionnee = signal<ReclamationCompagnie | null>(null);
  trajetEnEdition = signal<string | null>(null);

  brouillon: Omit<Trajet, 'id' | 'statut'> = {
    villeDepart: 'Abidjan',
    villeArrivee: 'Bouaké',
    heureDepart: '',
    prix: 0,
    placesDisponibles: 0,
  };

  readonly stats = computed(() => {
    const trajets = this.trajets();
    const reservations = this.reservations();
    const payees = reservations.filter(
      (reservation) => reservation.statut === 'CONFIRMEE' && reservation.paiement === 'PAYE',
    );

    return {
      trajetsActifs: trajets.filter((trajet) => trajet.statut === 'ACTIF').length,
      reservationsRecues: payees.length,
      placesVendues: payees.reduce((total, reservation) => total + reservation.tickets, 0),
      chiffreAffaires: payees.reduce((total, reservation) => total + reservation.total, 0),
      reclamationsEnAttente: this.reclamations().filter((reclamation) => reclamation.statut === 'EN_ATTENTE')
        .length,
    };
  });

  readonly trajetsFiltres = computed(() => {
    const motsCles = this.rechercheTrajet().trim().toLowerCase();
    if (!motsCles) return this.trajets();

    return this.trajets().filter((trajet) => {
      const contenu = [
        trajet.compagnie,
        trajet.villeDepart,
        trajet.villeArrivee,
        trajet.date,
        trajet.heureDepart,
        trajet.prix,
        trajet.placesDisponibles,
        trajet.statut,
      ]
        .filter((valeur) => valeur !== undefined && valeur !== null)
        .join(' ')
        .toLowerCase();

      return contenu.includes(motsCles);
    });
  });

  readonly reservationsFiltrees = computed(() => {
    const motsCles = this.rechercheReservation().trim().toLowerCase();
    if (!motsCles) return this.reservations();

    return this.reservations().filter((reservation) => {
      const contenu = [
        reservation.client,
        reservation.trajet,
        reservation.date,
        reservation.tickets,
        reservation.total,
        reservation.statut,
        reservation.paiement,
      ]
        .join(' ')
        .toLowerCase();

      return contenu.includes(motsCles);
    });
  });

  readonly reclamationsActives = computed(() =>
    this.reclamations().filter((reclamation) => reclamation.statut === 'EN_ATTENTE'),
  );

  ngOnInit(): void {
    this.chargerVitrine();
    this.chargerTrajets();
    this.chargerReservations();
  }

  surChangementLogo(evenement: Event): void {
    const input = evenement.target as HTMLInputElement;
    const fichier = input.files?.[0];
    if (!fichier) return;

    const lecteur = new FileReader();
    lecteur.onload = () => {
      this.logoApercu.set(lecteur.result as string);
      this.enregistrerVitrine();
    };
    lecteur.readAsDataURL(fichier);
  }

  surChangementCouverture(evenement: Event): void {
    const input = evenement.target as HTMLInputElement;
    const fichier = input.files?.[0];
    if (!fichier) return;

    const lecteur = new FileReader();
    lecteur.onload = () => {
      this.imageCouverture.set(lecteur.result as string);
      this.enregistrerVitrine();
    };
    lecteur.readAsDataURL(fichier);
  }

  surAjoutImages(evenement: Event): void {
    const input = evenement.target as HTMLInputElement;
    const fichiers = Array.from(input.files ?? []).slice(0, 6);
    if (!fichiers.length) return;

    fichiers.forEach((fichier) => {
      const lecteur = new FileReader();
      lecteur.onload = () => {
        this.galerieImages.update((images) => [...images, lecteur.result as string].slice(0, 6));
        this.enregistrerVitrine();
      };
      lecteur.readAsDataURL(fichier);
    });

    input.value = '';
  }

  supprimerImage(index: number): void {
    this.galerieImages.update((images) => images.filter((_, i) => i !== index));
    this.enregistrerVitrine();
  }

  ouvrirConfigurationVitrine(): void {
    this.formulaireVitrineOuvert.update((ouvert) => !ouvert);
  }

  enregistrerConfigurationVitrine(): void {
    this.enregistrerVitrine();
    this.formulaireVitrineOuvert.set(false);
  }

  ouvrirAjout(): void {
    this.trajetEnEdition.set(null);
    this.brouillon = {
      villeDepart: this.villes[0],
      villeArrivee: this.villes[1],
      heureDepart: '',
      prix: 0,
      placesDisponibles: 0,
    };
    this.formulaireOuvert.set(true);
  }

  ouvrirTousLesTrajets(): void {
    this.tousLesTrajetsOuverts.set(true);
  }

  fermerTousLesTrajets(): void {
    this.tousLesTrajetsOuverts.set(false);
  }

  ouvrirEdition(trajet: Trajet): void {
    this.trajetEnEdition.set(trajet.id);
    this.brouillon = {
      compagnie: trajet.compagnie,
      codeGareDepart: trajet.codeGareDepart,
      codeGareArrivee: trajet.codeGareArrivee,
      villeDepart: trajet.villeDepart,
      villeArrivee: trajet.villeArrivee,
      date: trajet.date,
      heureDepart: trajet.heureDepart,
      prix: trajet.prix,
      placesDisponibles: trajet.placesDisponibles,
    };
    this.formulaireOuvert.set(true);
  }

  annuler(): void {
    this.formulaireOuvert.set(false);
    this.trajetEnEdition.set(null);
  }

  enregistrer(): void {
    const idEnEdition = this.trajetEnEdition();
    const compagnie = this.nomCompagnie();
    const trajet = { ...this.brouillon, compagnie };

    if (idEnEdition) {
      this.vitrineService.modifierTrajet(idEnEdition, trajet).subscribe((modifie) => {
        this.trajets.update((liste) => liste.map((t) => (t.id === idEnEdition ? modifie : t)));
      });
    } else {
      this.vitrineService.creerTrajet(compagnie, trajet).subscribe((nouveau) => {
        this.trajets.update((liste) => [nouveau, ...liste]);
      });
    }

    this.formulaireOuvert.set(false);
    this.trajetEnEdition.set(null);
  }

  supprimer(id: string): void {
    this.vitrineService.supprimerTrajet(id).subscribe(() => {
      this.trajets.update((liste) => liste.filter((t) => t.id !== id));
    });
  }

  basculerStatut(id: string): void {
    this.vitrineService.basculerStatutTrajet(id).subscribe((trajet) => {
      this.trajets.update((liste) => liste.map((t) => (t.id === id ? trajet : t)));
    });
  }

  enregistrerVitrine(): void {
    this.vitrineService
      .enregistrerVitrine({
        compagnie: this.nomCompagnie(),
        description: this.description,
        logoUrl: this.logoApercu(),
        imageCouvertureUrl: this.imageCouverture(),
        galerieImages: this.galerieImages(),
        localisation: this.localisation,
        garesDesservies: this.lignesTexte(this.garesDesserviesTexte),
        flotte: this.lignesTexte(this.servicesTexte),
      })
      .subscribe((vitrine) => this.vitrine.set(vitrine));
  }

  repondreReclamation(id: string): void {
    this.reclamations.update((liste) =>
      liste.map((reclamation) =>
        reclamation.id === id && reclamation.reponse.trim()
          ? { ...reclamation, statut: 'REPONDUE' }
          : reclamation,
      ),
    );
  }

  resoudreReclamation(id: string): void {
    this.reclamations.update((liste) =>
      liste.map((reclamation) =>
        reclamation.id === id ? { ...reclamation, statut: 'RESOLUE' } : reclamation,
      ),
    );
    if (this.reclamationSelectionnee()?.id === id) {
      this.fermerDetailReclamation();
    }
  }

  ouvrirDetailReclamation(reclamation: ReclamationCompagnie): void {
    this.reclamationSelectionnee.set(reclamation);
  }

  fermerDetailReclamation(): void {
    this.reclamationSelectionnee.set(null);
  }

  nomCompagnie(): string {
    return this.authService.currentUser()?.nom || 'UTB';
  }

  private chargerVitrine(): void {
    this.vitrineService.obtenirVitrine(this.nomCompagnie()).subscribe((vitrine) => {
      this.vitrine.set(vitrine);
      this.description = vitrine.description;
      this.logoApercu.set(vitrine.logoUrl ?? null);
      this.imageCouverture.set(vitrine.imageCouvertureUrl ?? null);
      this.galerieImages.set(vitrine.galerieImages ?? []);
      this.localisation = vitrine.localisation ?? '';
      this.garesDesserviesTexte = (vitrine.garesDesservies ?? []).join('\n');
      this.servicesTexte = (vitrine.flotte ?? []).join('\n');
    });
  }

  private chargerTrajets(): void {
    this.vitrineService
      .listerTrajets(this.nomCompagnie())
      .subscribe((trajets) => this.trajets.set(trajets));
  }

  private chargerReservations(): void {
    this.vitrineService
      .listerReservations()
      .subscribe((reservations) => this.reservations.set(reservations));
  }

  private lignesTexte(valeur: string): string[] {
    return valeur
      .split('\n')
      .map((ligne) => ligne.trim())
      .filter(Boolean);
  }
}
