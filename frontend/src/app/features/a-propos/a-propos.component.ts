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

interface EtapeParcours {
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

  // ============================================================
  // PARCOURS D'UTILISATION (pipelines)
  // ============================================================
  readonly parcoursClient: EtapeParcours[] = [
    {
      titre: 'Je recherche',
      detail:
        'Je saisis départ, arrivée et date — au clavier, en langage libre ou à la voix (« dis où tu veux aller »).',
    },
    {
      titre: 'Je compare',
      detail:
        'Je vois toutes les compagnies, leurs tarifs et places dispo, avec l’affluence en temps réel et le conseil d’heure d’arrivée.',
    },
    {
      titre: 'Je réserve & je paie',
      detail: 'Je choisis mon trajet et je règle (Mobile Money / carte). Ma place est confirmée.',
    },
    {
      titre: 'Je reçois mon billet QR',
      detail: 'Un billet électronique signé apparaît dans mon espace — infalsifiable, toujours avec moi.',
    },
    {
      titre: 'J’embarque',
      detail:
        'À la gare, l’agent scanne mon QR : validé en une seconde. Fini la file d’attente et le billet papier.',
    },
    {
      titre: 'Je note mon voyage',
      detail: '24 h après, je reçois une invitation à noter la compagnie et la gare. La confiance se construit.',
    },
  ];

  readonly parcoursCompagnie: EtapeParcours[] = [
    {
      titre: 'Je crée mon compte compagnie',
      detail: 'Inscription avec vérification e-mail, puis accès à mon espace de gestion dédié.',
    },
    {
      titre: 'Je configure mon offre',
      detail: 'Je publie mes trajets, horaires, tarifs et capacités. Tout est visible immédiatement par les voyageurs.',
    },
    {
      titre: 'Je personnalise ma vitrine',
      detail: 'Logo, image de couverture, galerie : ma page publique attire et rassure les clients.',
    },
    {
      titre: 'Je reçois les réservations',
      detail: 'Je suis mon remplissage en temps réel et je consulte le tableau d’affluence de mes départs.',
    },
    {
      titre: 'Je contrôle à l’embarquement',
      detail: 'Mes agents scannent les QR des billets (en ligne ou hors-ligne) : anti-fraude, anti-double-usage.',
    },
    {
      titre: 'Je gagne en réputation',
      detail: 'Les notes de mes clients font monter mon badge de confiance et ma visibilité sur la plateforme.',
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
        'Réservation et paiement (Mobile Money / carte) de bout en bout',
      ],
    },
    {
      icone: '⏱️',
      titre: 'Annulation & remboursement',
      description: 'Le voyageur garde la main sur sa réservation, sans stress.',
      points: [
        'Annulation possible avec compte à rebours (fenêtre de ~30 min)',
        'Remboursement automatique à l’annulation',
        'Statut « Départ imminent » et masquage des départs déjà passés',
      ],
    },
    {
      icone: '🎫',
      titre: 'Billet électronique & contrôle',
      description:
        'Le billet papier disparaît : un QR signé, vérifiable à la gare, connexion ou pas.',
      points: [
        'Billet QR signé (RSA) dans l’espace client',
        'Écran de contrôle compagnie : scan caméra du QR',
        'Vérification hors-ligne par clé publique + anti-double-usage',
      ],
    },
    {
      icone: '🏢',
      titre: 'Espace compagnie & gestion intelligente',
      description:
        'Chaque compagnie pilote son activité depuis un tableau de bord dédié.',
      points: [
        'Gestion des trajets, horaires, tarifs et capacités',
        'Suivi des réservations et du taux de remplissage',
        'Tableau d’affluence pour anticiper les pics',
      ],
    },
    {
      icone: '🖼️',
      titre: 'Vitrine compagnie',
      description: 'Une page publique personnalisable pour attirer et rassurer les voyageurs.',
      points: [
        'Logo, image de couverture et galerie photos',
        'Présentation des trajets et de la note moyenne',
        'Une URL dédiée par compagnie',
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
      titre: 'Réclamations traitées par IA',
      description: 'Un canal de réclamation guidé par IA, avec suivi côté administration.',
      points: [
        'Assistant IA qui reformule et catégorise la réclamation',
        'Suivi du statut de traitement',
        'Escalade et transmission automatique à l’équipe support',
      ],
    },
    {
      icone: '🔐',
      titre: 'Compte & sécurité',
      description: 'Une base solide pour protéger voyageurs et compagnies.',
      points: [
        'Inscription avec vérification d’e-mail (code)',
        'Réinitialisation sécurisée du mot de passe + édition du compte',
        'Sessions JWT et limitation des tentatives de connexion',
      ],
    },
  ];
}
