import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';

interface Chiffre {
  valeur: string;
  libelle: string;
}

interface Fonctionnalite {
  icone: string;
  titre: string;
  description: string;
  points: string[];
}

interface EtapeDemo {
  numero: string;
  titre: string;
  detail: string;
}

@Component({
  selector: 'app-a-propos',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './a-propos.component.html',
  styleUrl: './a-propos.component.scss',
})
export class AProposComponent {
  // ============================================================
  // 1. PROBLEM IMPORTANCE — le problème, chiffré
  // ============================================================
  readonly chiffresProbleme: Chiffre[] = [
    { valeur: '15 %', libelle: 'du PIB ivoirien généré par le secteur des transports' },
    { valeur: '82 800 km', libelle: 'de réseau routier interurbain reliant Abidjan à l’intérieur' },
    { valeur: '17 M', libelle: 'de déplacements par jour dans le Grand Abidjan' },
    { valeur: '25 gares', libelle: 'pour la seule compagnie UTB — et autant de systèmes séparés' },
  ];

  /** Les symptômes de la fragmentation, tels que vécus par le voyageur. */
  readonly douleurs: string[] = [
    'Des dizaines de compagnies privées (UTB, CTE, CA-TRANS, Ocean CI, TSR…), chacune avec sa gare, éparpillées entre Adjamé, Yopougon et Koumassi — aucune vue d’ensemble.',
    'Des files d’attente interminables aux guichets, sans savoir si le car est plein ou s’il reste des places.',
    'Des billets papier faciles à perdre, à falsifier ou à revendre en double.',
    'Aucun historique ni traçabilité : impossible de retrouver un trajet passé ou de noter une compagnie.',
    'Se déplacer jusqu’à la gare juste pour découvrir que le départ est complet… ou déjà parti.',
  ];

  // ============================================================
  // 2. LA SOLUTION — le guichet unique
  // ============================================================
  readonly piliers: { titre: string; texte: string }[] = [
    {
      titre: 'Centraliser',
      texte:
        'Toutes les compagnies interurbaines et leurs départs réunis sur une seule plateforme. Un compte, une recherche, tous les trajets de Côte d’Ivoire.',
    },
    {
      titre: 'Fluidifier',
      texte:
        'Anticiper l’affluence, proposer une file virtuelle et conseiller la meilleure heure d’arrivée pour supprimer l’attente à la gare.',
    },
    {
      titre: 'Sécuriser',
      texte:
        'Un billet électronique signé cryptographiquement, vérifiable à l’embarquement — même sans connexion — et infalsifiable.',
    },
    {
      titre: 'Faire confiance',
      texte:
        'Des notes vérifiées sur les compagnies et les gares, un historique complet et un support assisté par IA.',
    },
  ];

  // ============================================================
  // 3. INNOVATION — ce qui n'existe nulle part ailleurs
  // ============================================================
  readonly innovations: Fonctionnalite[] = [
    {
      icone: '🚦',
      titre: 'Intelligence anti-file d’attente',
      description:
        'Le cœur innovant : prévoir l’affluence pour que le voyageur arrive au bon moment et ne fasse plus la queue.',
      points: [
        'Prédiction du niveau d’affluence de la gare (faible / moyenne / forte)',
        'Carte de chaleur de l’affluence par compagnie et par créneau',
        'File virtuelle : prendre son tour à distance, sans patienter sur place',
      ],
    },
    {
      icone: '🤖',
      titre: 'Assistant IA conversationnel (Google Gemini)',
      description:
        'Le voyageur interroge l’affluence et planifie son trajet en langage naturel.',
      points: [
        'Recherche en langage libre (« je veux aller à Bouaké demain, pas trop cher »)',
        'Conseils d’heure d’arrivée personnalisés',
        'Enrichissement contextuel : jours fériés, pics de trafic détectés par l’IA',
      ],
    },
    {
      icone: '🎟️',
      titre: 'Billet QR signé anti-fraude',
      description:
        'Un QR code signé en RSA qui remplace le billet papier et se vérifie à la gare, connexion ou pas.',
      points: [
        'Signature cryptographique RS256 : impossible à falsifier ou à recréer',
        'Vérification hors-ligne par clé publique quand le réseau manque',
        'Anti-double-usage : un billet validé ne passe pas deux fois',
      ],
    },
    {
      icone: '🎙️',
      titre: 'Dictée vocale & accessibilité',
      description:
        'On dit où l’on veut aller, la recherche se lance — pensé pour tous les niveaux de littératie.',
      points: [
        '« Dis où tu veux aller » → recherche vocale instantanée',
        'Interface responsive, pensée mobile d’abord',
      ],
    },
  ];

  // ============================================================
  // 4. IMPACT POTENTIAL — modèle, échelle, pérennité
  // ============================================================
  readonly impacts: { titre: string; texte: string }[] = [
    {
      titre: 'Un modèle économique clair',
      texte:
        'Commission sur chaque billet vendu + abonnement SaaS pour les compagnies (vitrine en ligne, gestion des trajets, tableau de bord d’affluence).',
    },
    {
      titre: 'Un marché immense, déjà en digitalisation',
      texte:
        'Des dizaines de compagnies, des millions de trajets par an. Les compagnies adoptent déjà la billetterie électronique : GareConnect fédère ce mouvement.',
    },
    {
      titre: 'Un passage à l’échelle naturel',
      texte:
        'D’Abidjan à toutes les gares de Côte d’Ivoire, puis à la sous-région (des opérateurs comme TSR font déjà du transfrontalier).',
    },
    {
      titre: 'De la valeur pour tout l’écosystème',
      texte:
        'Moins d’attente et de fraude pour les voyageurs ; des données de fréquentation et une visibilité en ligne pour les compagnies ; plus de sécurité pour les gares.',
    },
  ];

  // ============================================================
  // 5. TECHNICAL EXCELLENCE — architecture & sécurité
  // ============================================================
  readonly technologies: { nom: string; role: string }[] = [
    { nom: 'Angular 18', role: 'Application web (front)' },
    { nom: 'Spring Boot 3', role: 'API & logique métier (back)' },
    { nom: 'PostgreSQL / Supabase', role: 'Base de données' },
    { nom: 'Google Gemini', role: 'Intelligence artificielle' },
    { nom: 'Vercel · Fly.io', role: 'Déploiement en production' },
  ];

  readonly securite: string[] = [
    'Authentification par jeton JWT (sessions sans état)',
    'Billets signés RSA / RS256, vérifiables hors-ligne par clé publique',
    'Anti-double-usage des billets + validation scopée à la compagnie de l’agent',
    'Limitation des tentatives de connexion (anti-force brute)',
    'Inscription avec vérification d’e-mail + réinitialisation sécurisée du mot de passe',
    'Architecture « cœur déterministe + surcouche IA avec repli » : l’IA n’interrompt jamais le service',
  ];

  // ============================================================
  // EXECUTION QUALITY — tester la démo (prototype fonctionnel)
  // ============================================================
  readonly demoClient = { email: 'demo-affluence@gareconnect.local', mdp: 'DemoPass123!' };
  readonly demoCompagnie = { email: 'demo-compagnie@gareconnect.local', mdp: 'DemoPass123!' };

  readonly etapesDemo: EtapeDemo[] = [
    {
      numero: '1',
      titre: 'Côté voyageur',
      detail:
        'Connecte-toi avec le compte client, recherche un trajet, réserve et ouvre ton billet : un QR code signé apparaît.',
    },
    {
      numero: '2',
      titre: 'Côté compagnie',
      detail:
        'Connecte-toi avec le compte compagnie, va dans « Contrôle billets » et scanne le QR du voyageur → « Billet validé ». Rescanne-le → « Déjà utilisé ».',
    },
    {
      numero: '3',
      titre: 'L’intelligence en action',
      detail:
        'Sur la page d’accueil, observe la prédiction d’affluence en temps réel et essaie l’assistant IA en langage libre.',
    },
  ];

  /** Le catalogue complet, groupé par domaine (référence exhaustive). */
  readonly fonctionnalites: Fonctionnalite[] = [
    {
      icone: '🔎',
      titre: 'Recherche & réservation centralisées',
      description:
        'Le guichet unique du voyageur : comparer et réserver chez n’importe quelle compagnie sans multiplier les comptes.',
      points: [
        'Recherche par ville de départ, ville d’arrivée et date',
        'Comparaison des tarifs et des places disponibles en temps réel',
        'Réservation et paiement (Mobile Money / carte) simulés de bout en bout',
        'Annulation avec compte à rebours et remboursement',
        'Statut « Départ imminent » et masquage des départs déjà passés',
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
        'Suivi des réservations et écran de contrôle des billets à l’embarquement',
      ],
    },
    {
      icone: '⭐',
      titre: 'Confiance & notation',
      description:
        'Des avis vérifiés qui récompensent les compagnies sérieuses et guident les voyageurs.',
      points: [
        'Notation des compagnies et des gares, réservée aux voyages réellement effectués',
        'Notification automatique 24 h après le voyage pour inviter à noter',
        'Badge de note moyenne affiché sur chaque compagnie',
      ],
    },
    {
      icone: '🛟',
      titre: 'Support & réclamations assistés par IA',
      description: 'Un canal de réclamation guidé par IA, avec suivi côté administration.',
      points: [
        'Assistant IA qui reformule et catégorise la réclamation',
        'Suivi du statut de traitement et transmission à l’équipe support',
      ],
    },
  ];
}
