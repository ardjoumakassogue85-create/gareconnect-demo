# GareConnect — Présentation générale

> Plateforme web de réservation et d'intelligence pour les **gares routières
> interurbaines** de Côte d'Ivoire. Elle aide le voyageur à trouver, comparer et
> réserver un trajet — et surtout à **éviter la file d'attente** — tout en donnant
> aux compagnies une vitrine professionnelle et des données pour piloter leur offre.

---

## 1. Le problème

Dans les gares de transport interurbain (les « gares routières »), l'expérience est
subie plutôt que maîtrisée :

**Côté voyageur**
- **Files d'attente longues et imprévisibles** : personne ne sait à quelle heure la
  gare sera bondée. On arrive « au hasard » et on subit la queue au guichet.
- **Aucune comparaison simple** entre gares et compagnies (prix, horaires, fiabilité).
- **Manque d'information officielle et à jour**, surtout pour les compagnies peu
  connues qui n'ont aucune présence en ligne crédible.

**Côté compagnie**
- **Faible visibilité / image de marque**, particulièrement pour les petites compagnies.
- **Pas d'outil simple** pour centraliser et communiquer leurs informations
  (gares, horaires, tarifs, logo).
- **Aucune donnée sur l'affluence prévisible** pour anticiper les ressources.

---

## 2. La solution

**GareConnect** centralise l'offre de plusieurs compagnies, aide le voyageur à
choisir grâce à un **moteur de recommandation assisté par IA**, et donne à chaque
compagnie une **vitrine** et un **tableau de bord data**. Le tout avec réservation
en ligne et un axe fort : l'**anti-file d'attente**.

La philosophie technique : un **cœur déterministe** fiable (scoring, règles métier),
et l'**IA (Gemini) en surcouche** pour le langage, le raisonnement et le contexte —
avec repli automatique pour que la démo ne casse jamais.

---

## 3. Utilisateurs cibles

| Profil | Besoin principal |
|--------|------------------|
| **Voyageur** | Trouver vite le meilleur trajet (gare, horaire, tarif), réserver, éviter la file |
| **Compagnie** | Gagner en visibilité, présenter ses gares/services, gérer son offre, lire la demande |

---

## 4. Toutes les fonctionnalités

### 4.1 Comptes & sécurité
- **Inscription** avec deux rôles : **client** et **compagnie** (Spring Security + JWT).
- **Validation du compte par email** : un code est envoyé à l'inscription ; tant que
  l'email n'est pas vérifié, la connexion est bloquée (compte non activé).
- **Connexion** avec option « se souvenir de moi » (jeton de plus longue durée).
- **Mot de passe oublié — sécurisé** : envoi par email d'un **lien de réinitialisation
  à jeton** à durée limitée (30 min). On ne renvoie jamais de mot de passe en clair.
- **Modification du compte** (`/mon-compte`) : nom, email, mot de passe.

### 4.2 Recherche & assistant IA
- **Recherche structurée** : ville de départ, ville d'arrivée, date.
- **Recherche en langage libre** : l'utilisateur décrit son besoin (« je veux partir
  demain matin pas trop cher vers Bouaké »). **Gemini** extrait les critères
  (villes, date, heure, budget, tri…), avec **repli heuristique local** si l'IA est
  indisponible.
- **Dictée vocale** 🎙️ : bouton micro « dis où tu veux aller » (Web Speech API,
  fr-FR) qui remplit la recherche à la voix — dégradation gracieuse si non supporté.

### 4.3 Réservation & billet
- **Réservation** avec **paiement simulé** : Mobile Money (Wave, Orange Money,
  MTN, Moov) ou carte. Gestion des places disponibles.
- **Billet avec QR code**.
- **Annulation possible pendant 30 minutes** après la réservation (remboursement simulé).

### 4.4 Espace voyageur
- Billets classés : **à venir / passés / annulés / tous**.
- Statuts calculés : Confirmée, Terminée, Annulée.
- **Notation** du voyage (1–5) et **avis sur la gare**.

### 4.5 Espace compagnie
- **Tableau de bord** : trajets actifs, réservations reçues, places vendues, CA,
  réclamations à traiter.
- **Gestion des trajets (CRUD)** : ajout, modification, suspension, suppression.
- **Configuration de la vitrine** : logo, image de couverture, galerie, description,
  localisation, gares desservies, flotte/services (images stockées en base).
- **Réservations reçues** et **réclamations** clients.
- **Heatmap d'affluence** (voir §4.7).

### 4.6 Vitrine publique & réputation
- **Page vitrine dynamique par compagnie** (template unique alimenté par les données saisies).
- **Vitrine publique des trajets** de la compagnie.
- **Badge de notation** de la compagnie (note moyenne + nombre d'avis), **mis à jour
  automatiquement** à chaque notation d'un voyage.
- **Avis sur les gares** (note moyenne par gare).

### 4.7 Anti-file d'attente — l'intelligence d'affluence ⭐ (fonctionnalité phare)
> Détail complet : [anti-file-attente.md](anti-file-attente.md) et
> [anti-file-attente-ia.md](anti-file-attente-ia.md).

- **Prédiction d'affluence** par gare et par créneau horaire, en mélangeant le
  **signal réel** (réservations agrégées) et une **courbe type** des heures de pointe.
  Le mélange est **adaptatif** : crédible dès le premier jour, factuel dès qu'il y a
  des données (indice de « confiance » affiché).
- **Côté voyageur** : bannière d'affluence + mini-graphe, **badge FORTE/MOYENNE/FAIBLE**
  par trajet, et conseil **« arrive vers HH:MM »** (marge qui grandit avec l'affluence).
- **Côté compagnie** : **heatmap 7 jours × créneaux** + **recommandations** d'ouverture
  de départs là où la demande est forte et l'offre absente.
- **Masquage des trajets expirés** (partis) et statut **« Départ imminent »** (≤ 30 min).

**Les 3 briques IA de l'anti-file :**
1. **Assistant conversationnel anti-file** : le voyageur parle/écrit, le déterministe
   classe les départs par « coût de file », **Gemini choisit le plus calme et rédige
   un conseil humain** (« prends le 20h30, arrive à 20h10, tu évites la foule »).
2. **File virtuelle « coupe-file »** : au lieu d'informer, on **agit** — on attribue
   au voyageur la **fenêtre d'arrivée la moins chargée** pour lisser le pic.
3. **Enrichissement contextuel** : Gemini détecte un **jour férié / événement** (ex.
   Fête nationale → +70%) que la donnée seule ignore, et prévient d'arriver plus tôt.

### 4.8 Notifications
- **Rappel « Note ton voyage »** généré **24h après le trajet** par un job planifié :
  le voyageur reçoit une notification l'invitant à noter la compagnie sur 5.
- **Cloche 🔔** dans l'en-tête (badge de non-lues) ; cliquer ouvre directement la notation.
- La note alimente le **badge de la compagnie** (§4.6).

### 4.9 Réclamations (assistées par IA)
- Le voyageur ouvre une réclamation ; une **réponse IA** est proposée (via un
  connecteur Make.com), avec escalade possible vers un traitement humain côté compagnie.

### 4.10 Collecte de données & PWA
- Chaque **recherche** et **réservation** est loguée → base pour affiner la prédiction d'affluence.
- **PWA** : application installable (manifest + service worker), pensée **mobile-first**.

---

## 5. Stack technique & architecture

| Couche | Technologie |
|--------|-------------|
| Frontend | **Angular 18** (PWA), TypeScript, signals |
| Backend | **Spring Boot 3**, Spring Security, **JWT** |
| Base de données | **Supabase** (PostgreSQL) |
| IA générative | **Google Gemini** (recherche NL, assistant anti-file, contexte) |
| Reconnaissance vocale | **Web Speech API** (navigateur) |
| Réclamations IA | Connecteur **Make.com** |

**Principe d'architecture** : logique métier déterministe (services Spring) + surcouche
IA défensive (`GeminiClient` : timeout court, repli automatique). Les endpoints publics
(recherche, affluence, assistant) et protégés (réservation, notifications, espace
compagnie) sont séparés par rôle dans Spring Security.

---

## 6. Déploiement

- **GitHub** : code source.
- **Vercel** : frontend Angular.
- **Render / Railway** : backend Spring Boot.
- **Supabase** : base de données + stockage.

Secrets (mot de passe DB, `JWT_SECRET`, SMTP, `GEMINI_API_KEY`, webhook Make) dans
`backend/.env` (ignoré par Git) et dans les variables d'environnement de l'hébergeur.

---

## 7. Lancer en local

**Backend** (Java 17 + Maven, base H2 en repli si Supabase non configuré) :
```bash
cd backend
mvn spring-boot:run        # API sur http://localhost:8081/api
```

**Frontend** (Node 18+) :
```bash
cd frontend
npm install
npm start                  # http://localhost:4200
```

> Données de démonstration : `SEED_DEMO=true` dans `backend/.env` génère trajets +
> réservations pour peupler l'affluence (voir [anti-file-attente.md](anti-file-attente.md)).
> Comptes démo : client `demo-affluence@gareconnect.local` / `DemoPass123!`,
> compagnie `demo-compagnie@gareconnect.local` / `DemoPass123!`.

---

## 8. Parcours de démo recommandé

1. **Voyageur** : recherche (ou dictée vocale) → **bannière d'affluence** + badges →
   **assistant anti-file** conseille le meilleur départ → **créneau coupe-file** →
   réservation → **billet QR**.
2. **24h après** (démo) : la **cloche** affiche un rappel → notation → le **badge de la
   compagnie** se met à jour.
3. **Compagnie** : dashboard → **heatmap d'affluence** + recommandations → configuration
   de la **vitrine** → vitrine publique visible.

---

## 9. Documentation détaillée

- [anti-file-attente.md](anti-file-attente.md) — l'intelligence d'affluence (moteur, API, données démo).
- [anti-file-attente-ia.md](anti-file-attente-ia.md) — les 3 briques IA (assistant, file virtuelle, contexte).

---

## 10. Le pitch en une phrase

> *GareConnect ne se contente pas de vendre des billets : elle prédit l'affluence,
> **négocie avec chaque voyageur le meilleur moment pour venir**, dit à la compagnie
> **quand mettre un bus**, et transforme chaque voyage en avis pour bâtir la confiance —
> pour que plus personne ne fasse la queue à l'aveugle.*
