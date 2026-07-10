import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';

interface Fonctionnalite {
  icone: string;
  titre: string;
  description: string;
  points: string[];
}

@Component({
  selector: 'app-a-propos',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './a-propos.component.html',
  styleUrl: './a-propos.component.scss',
})
export class AProposComponent {
  /** Les symptômes de la fragmentation, tels que vécus par le voyageur. */
  readonly douleurs: string[] = [
    "Aucune vue d'ensemble : chaque compagnie a sa propre gare, ses horaires, ses tarifs — introuvables au même endroit.",
    "Des files d'attente interminables aux guichets, sans savoir si le car est plein ou s'il reste des places.",
    "Des billets papier faciles à perdre, à falsifier ou à revendre en double.",
    "Aucun historique, aucun suivi : impossible de retrouver un trajet passé ou de noter une compagnie.",
    "Se déplacer à la gare juste pour découvrir que le départ est complet ou déjà parti.",
  ];

  /** Les piliers de la solution : un guichet unique numérique. */
  readonly piliers: { titre: string; texte: string }[] = [
    {
      titre: 'Centraliser',
      texte:
        "Toutes les compagnies interurbaines et leurs départs réunis sur une seule plateforme. Un compte, une recherche, tous les trajets de Côte d'Ivoire.",
    },
    {
      titre: 'Fluidifier',
      texte:
        "Anticiper l'affluence, proposer une file virtuelle et conseiller la meilleure heure d'arrivée pour supprimer l'attente à la gare.",
    },
    {
      titre: 'Sécuriser',
      texte:
        "Un billet électronique signé cryptographiquement, vérifiable à l'embarquement — même sans connexion — et infalsifiable.",
    },
    {
      titre: 'Faire confiance',
      texte:
        "Des notes vérifiées sur les compagnies et les gares, un historique complet et un support assisté par IA.",
    },
  ];

  /** Le catalogue complet, groupé par domaine. */
  readonly fonctionnalites: Fonctionnalite[] = [
    {
      icone: '🔎',
      titre: 'Recherche & réservation centralisées',
      description:
        "Le guichet unique du voyageur : comparer et réserver chez n'importe quelle compagnie sans multiplier les comptes.",
      points: [
        'Recherche par ville de départ, ville d’arrivée et date',
        'Comparaison des tarifs et des places disponibles en temps réel',
        'Réservation et paiement (Mobile Money / carte) simulés de bout en bout',
        'Annulation avec compte à rebours et remboursement',
        'Masquage automatique des départs déjà passés',
        'Statut « Départ imminent » pour les trajets à moins de 30 min',
      ],
    },
    {
      icone: '🎟️',
      titre: 'Billet électronique anti-fraude',
      description:
        'Un QR code signé numériquement qui remplace le billet papier et se vérifie à la gare, connexion ou pas.',
      points: [
        'QR signé en RSA (RS256) : impossible à falsifier ou à recréer',
        "Écran de contrôle pour l'agent : scan caméra du QR client",
        'Vérification hors-ligne par clé publique quand le réseau manque',
        'Anti-double-usage : un billet validé ne passe pas deux fois',
        "Validation limitée à la compagnie de l'agent (chacun ne scanne que ses billets)",
        'Expiration automatique du billet après la date du voyage',
      ],
    },
    {
      icone: '🚦',
      titre: 'Anti-file d’attente intelligent',
      description:
        "Le cœur innovant : prévoir l'affluence pour que le voyageur arrive au bon moment et ne fasse plus la queue.",
      points: [
        "Prédiction du niveau d'affluence de la gare (faible / moyenne / forte)",
        'Carte de chaleur de l’affluence par compagnie et par créneau',
        "Conseil personnalisé sur l'heure d'arrivée idéale",
        'File virtuelle : prendre son tour à distance, sans patienter sur place',
        'Assistant conversationnel IA pour interroger l’affluence en langage naturel',
        'Enrichissement contextuel (jours fériés, pics de trafic) via IA',
      ],
    },
    {
      icone: '🏢',
      titre: 'Espace & vitrine compagnie',
      description:
        'Chaque compagnie gère son offre et sa présence en ligne depuis un tableau de bord dédié.',
      points: [
        'Gestion des trajets, horaires, tarifs et capacités',
        'Vitrine publique personnalisable (logo, image de couverture, galerie)',
        'Suivi des réservations et du remplissage',
        "Écran de contrôle des billets à l'embarquement",
      ],
    },
    {
      icone: '⭐',
      titre: 'Confiance & notation',
      description:
        'Des avis vérifiés qui récompensent les compagnies sérieuses et guident les voyageurs.',
      points: [
        'Notation des compagnies sur 5 étoiles, réservée aux voyages réellement effectués',
        "Notation de l'accueil en gare",
        'Notification automatique 24 h après le voyage pour inviter à noter',
        'Badge de note moyenne affiché sur chaque compagnie',
      ],
    },
    {
      icone: '🛟',
      titre: 'Support & réclamations assistés par IA',
      description:
        'Un canal de réclamation guidé par IA, avec suivi côté administration.',
      points: [
        'Assistant IA qui reformule et catégorise la réclamation',
        'Suivi du statut de traitement',
        "Transmission automatique vers l'équipe support",
      ],
    },
    {
      icone: '🔐',
      titre: 'Compte & sécurité',
      description:
        'Une base solide pour protéger les comptes et les données des voyageurs et des compagnies.',
      points: [
        'Inscription avec vérification de l’adresse e-mail',
        'Réinitialisation de mot de passe sécurisée par lien à usage unique',
        'Édition du compte (nom, e-mail, mot de passe)',
        'Authentification par jeton JWT (sessions sans état)',
        'Limitation du nombre de tentatives de connexion (anti-force brute)',
      ],
    },
    {
      icone: '🎙️',
      titre: 'Expérience & accessibilité',
      description:
        'Des raccourcis pensés pour tous, y compris à l’oral.',
      points: [
        'Dictée vocale : « dis où tu veux aller » et la recherche se lance',
        'Recherche en langage libre interprétée automatiquement',
        'Interface responsive, pensée mobile',
        'Design inspiré des gares : tableaux de départ, tickets, signalétique',
      ],
    },
  ];

  /** Stack technique, pour la partie jury / technique. */
  readonly technologies: { nom: string; role: string }[] = [
    { nom: 'Angular 18', role: 'Application web (front)' },
    { nom: 'Spring Boot 3', role: 'API & logique métier (back)' },
    { nom: 'PostgreSQL / Supabase', role: 'Base de données' },
    { nom: 'Google Gemini', role: 'Intelligence artificielle' },
    { nom: 'JWT + RSA', role: 'Sécurité & billets signés' },
  ];
}
