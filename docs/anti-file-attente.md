# Anti-file d'attente — Intelligence d'affluence

> Fonctionnalité phare de GareConnect : estimer l'affluence en gare par créneau
> horaire pour **éviter les files d'attente**, conseiller le voyageur sur la
> meilleure heure d'arrivée, et donner aux compagnies une carte de la demande.
>
> 👉 Les **briques IA** (assistant conversationnel, file virtuelle, enrichissement
> contextuel) sont documentées dans [anti-file-attente-ia.md](anti-file-attente-ia.md).

---

## 1. Le problème

Le cahier des charges identifie comme problème n°1 des gares interurbaines :

- **Files d'attente longues et imprévisibles**, sans visibilité sur l'affluence ;
- côté compagnies, **aucune donnée sur l'affluence prévisible** pour anticiper les ressources.

Il annonce aussi la démarche : _« chaque recherche/réservation est loguée (base
pour une future prédiction d'affluence) … la collecte démarre, le modèle viendra
après »_. **Cette fonctionnalité livre ce modèle.**

---

## 2. L'approche : deux signaux mélangés

L'affluence d'un créneau (gare × jour de semaine × heure) est estimée en combinant :

1. **Signal réel** — les tickets déjà réservés, agrégés depuis les réservations
   confirmées (données réellement collectées par l'app).
2. **A priori métier** — la courbe type des départs interurbains : pointe du matin
   (06 h–08 h), creux de midi, seconde pointe en fin d'après-midi (17 h–18 h),
   pondérée par le jour (vendredi / dimanche plus chargés).

Le mélange est **adaptatif** via un coefficient de confiance `alpha` :

```
alpha  = min(1, total_tickets_gare / 40)
prior  = min(100, PRIOR[heure] × multiplicateur_jour)
reel   = min(100, 100 × tickets_creneau / 18)
score  = (1 − alpha) × prior  +  alpha × reel      // borné 0..100
```

- **Peu de données** (`alpha → 0`) : l'a priori domine → courbe crédible dès le 1er jour.
- **Beaucoup de données** (`alpha → 1`) : le réel prend le dessus → vraie affluence observée.

Le champ `confiance` (= `alpha × 100`) est renvoyé à l'UI : il affiche honnêtement
la part de données réelles dans l'estimation.

### Niveaux

| Score | Niveau  |
|------:|---------|
| < 34  | FAIBLE  |
| < 67  | MOYENNE |
| ≥ 67  | FORTE   |

### Conseil « meilleur moment pour arriver »

Marge d'arrivée avant le départ, qui grandit avec l'affluence :

| Niveau au départ | Arriver avant |
|------------------|--------------:|
| FAIBLE           | 20 min        |
| MOYENNE          | 35 min        |
| FORTE            | 50 min        |

`heure_conseillée = heure_départ − marge`.

---

## 3. Architecture

```
                 ┌──────────────────────────────────────────┐
                 │            AffluenceService (BE)           │
   Reservations ─┤  agrège tickets (ville×jour×heure)         │
   (confirmées)  │  + courbe a priori + mélange adaptatif     │
                 │  → score/niveau par créneau                │
                 └───────────────┬───────────────┬────────────┘
                                 │               │
             GET /api/affluence/gare      GET /api/compagnies/me/affluence
             (public, voyageur)           (rôle COMPAGNIE)
                                 │               │
                 ┌───────────────▼──┐   ┌────────▼─────────────────┐
                 │  Résultats (FE)  │   │  Espace compagnie (FE)   │
                 │  bannière + graphe│   │  heatmap 7j × créneaux   │
                 │  badge + « arrive │   │  + recommandations       │
                 │  vers HH:MM »     │   │                          │
                 └───────────────────┘   └──────────────────────────┘
```

---

## 4. Backend (Spring Boot)

### 4.1 Service — `service/AffluenceService.java`

Cœur de la fonctionnalité. Points clés :

- **Constantes** : `HEURE_DEBUT=5`, `HEURE_FIN=21`, `TICKETS_REF=18`
  (tickets/heure = saturation du signal réel), `SEUIL_CONFIANCE=40`
  (total tickets gare pour `alpha=1`).
- **Courbe a priori** `PRIOR[24]` (0..100 par heure) : pics à 07 h (100) et 18 h (94).
- **Multiplicateur de jour** : vendredi ×1.15, dimanche ×1.18, samedi ×1.05, sinon ×1.0.
- **Classe interne `ProfilAffluence`** : pré-agrège la table `(jour×100+heure) → tickets`
  et expose `score(jour, heure)` + `confiance()`.

Méthodes publiques :

| Méthode | Rôle |
|---------|------|
| `affluenceGare(ville, date)` | Profil horaire d'une gare pour un jour → `AffluenceGareDto` |
| `affluenceCompagnie(compagnie)` | Heatmap 7 j × créneaux 2 h + suggestions → `AffluenceCompagnieDto` |
| `conseilArrivee(heureDepart, niveau)` | Heure d'arrivée conseillée (départ − marge) |

**Heatmap compagnie** : créneaux de 2 h (`05-07 … 19-21`) sur 7 jours ; l'offre
actuelle (départs `ACTIF` non expirés) est comparée à la demande estimée. Les
**suggestions** ciblent les créneaux à forte demande (score ≥ 60) où la compagnie
n'a aucun départ (top 3).

### 4.2 DTOs — `dto/`

- `CreneauAffluenceDto(heure, score, niveau)`
- `AffluenceGareDto(gare, date, jour, niveauGlobal, heureLaPlusChargee, creneauLePlusCalme, confiance, creneaux[])`
- `LigneHeatmapDto(jour, creneaux[])`
- `AffluenceCompagnieDto(gares[], creneaux[], heatmap[], suggestions[])`

### 4.3 Contrôleurs

- `controller/AffluenceController.java` → `GET /api/affluence/gare` (public).
- `controller/CompagnieController.java` → `GET /api/compagnies/me/affluence` (ajout, rôle COMPAGNIE).

### 4.4 Repository — `repository/ReservationRepository.java`

Ajout : `findByVilleDepartIgnoreCaseAndStatut(ville, StatutReservation.CONFIRMEE)`.

### 4.5 Sécurité — `config/SecurityConfig.java`

Ajout : `.requestMatchers("/api/affluence/**").permitAll()` (le endpoint compagnie
reste protégé sous `/api/compagnies/**` → `hasRole("COMPAGNIE")`).

---

## 5. API REST

### `GET /api/affluence/gare`

| Paramètre | Requis | Description |
|-----------|:------:|-------------|
| `ville`   | oui    | Ville de la gare de départ |
| `date`    | non    | `yyyy-MM-dd` (défaut : aujourd'hui) |

**Exemple**

```
GET /api/affluence/gare?ville=Abidjan
```

```json
{
  "gare": "Gare de Abidjan",
  "date": "2026-07-08",
  "jour": "Mercredi",
  "niveauGlobal": "MOYENNE",
  "heureLaPlusChargee": "07:00",
  "creneauLePlusCalme": "21:00",
  "confiance": 18,
  "creneaux": [
    { "heure": "05:00", "score": 45, "niveau": "MOYENNE" },
    { "heure": "07:00", "score": 83, "niveau": "FORTE" },
    { "heure": "18:00", "score": 78, "niveau": "FORTE" },
    { "heure": "21:00", "score": 31, "niveau": "FAIBLE" }
  ]
}
```

### `GET /api/compagnies/me/affluence` _(JWT rôle COMPAGNIE)_

```json
{
  "gares": ["Abidjan", "Bouaké"],
  "creneaux": ["05:00-07:00", "07:00-09:00", "…", "19:00-21:00"],
  "heatmap": [
    { "jour": "Lundi", "creneaux": [ { "heure": "05:00-07:00", "score": 61, "niveau": "MOYENNE" }, … ] },
    …
  ],
  "suggestions": [
    "Forte demande Vendredi sur le creneau 17:00-19:00 (affluence 88%) et aucun de tes departs : envisage d'en ajouter un."
  ]
}
```

---

## 6. Frontend (Angular)

### 6.1 Modèle & service

- `core/models/affluence.model.ts` — types `NiveauAffluence`, `CreneauAffluence`,
  `AffluenceGare`, `LigneHeatmap`, `AffluenceCompagnie`.
- `core/services/affluence.service.ts` — `gare(ville, date?)`, `compagnie()`, et
  `heureArrivee(heureDepart, niveau)` (calcul de la marge côté client).

### 6.2 Voyageur — `features/recherche/resultats.component`

- **Bannière d'affluence** au-dessus des résultats : niveau global, pic, créneau
  le plus calme, % de confiance, et un **mini-graphe** en barres par créneau.
- Par trajet : **badge d'affluence** (colonne Heure) + conseil **« arrive vers HH:MM »**.
- Chargée après chaque recherche (classique et IA) via `chargerAffluence()`.

### 6.3 Compagnie — `features/espace-compagnie/espace-compagnie.component`

- **Heatmap** 7 jours × créneaux 2 h (cellules colorées par niveau) juste sous les stats.
- Bloc **Recommandations** (suggestions du backend).
- Chargée dans `ngOnInit()` via `chargerAffluence()`.

---

## 7. Tester / démontrer

### API (curl)

```bash
# Voyageur — profil d'une gare
curl "http://localhost:8081/api/affluence/gare?ville=Abidjan"

# Un vendredi (multiplicateur de jour)
curl "http://localhost:8081/api/affluence/gare?ville=Bouake&date=2026-07-10"
```

### Navigateur

- **Voyageur** : `http://localhost:4200` → connexion client → **Rechercher**
  (ex. Abidjan) → la bannière et les badges d'affluence apparaissent.
- **Compagnie** : connexion compagnie → dashboard → la **heatmap** est sous les stats.

### Le pitch (boucle complète)

> Problème → donnée → décision, des deux côtés :
> - Voyageur : « la gare est bondée à 08 h, arrive à 07 h 10 ».
> - Compagnie : « forte demande vendredi 18 h, ouvre un départ ».

### Données de démonstration (`SEED_DEMO`)

Pour rendre la fonctionnalité **visible immédiatement**, un seeder alimente la base
avec un jeu réaliste :

- Fichier : `config/DemoAffluenceSeeder.java` (activé par `app.seed-demo`, env `SEED_DEMO=true`).
- Contenu : une compagnie + un client de démo, **300 trajets** sur **10 routes** entre
  les 5 grandes villes (Abidjan, Bouaké, Yamoussoukro, San-Pédro, Korhogo) à des heures
  variées (`06:30, 07:30, 12:00, 15:00, 17:30, 20:30`), et **595 réservations** suivant
  une distribution horaire réaliste (pics 07-08 h et 17-18 h) sur les 7 derniers jours.
- **Noms de villes avec accents exacts** (Bouaké, San-Pédro) pour matcher la recherche
  du frontend, qui est sensible aux accents.
- **Intensité propre à chaque ville** → profils distincts : une grande ville est plus
  chargée qu'une petite.
- **Idempotent & rafraîchissant** : à chaque démarrage il nettoie puis régénère
  (les trajets restent donc toujours à des dates futures, non expirées).

Effet observable — toutes les gares sont alimentées, avec des niveaux différents :

| Gare (départ) | Pic 07 h | Lecture |
|---------------|----------|---------|
| Abidjan       | **78 — FORTE**   | Capitale, gare très chargée aux heures de pointe |
| Bouaké        | ~74 — FORTE      | Grande ville |
| Yamoussoukro  | ~70 — FORTE      | |
| San-Pédro     | ~66 — MOYENNE    | |
| Korhogo       | **61 — MOYENNE** | Ville plus petite, gare plus calme |

Toutes affichent **100 % de confiance** (pilotées par les réservations). Une gare
sans historique retomberait automatiquement sur la seule courbe a priori (confiance 0 %),
ce qui **démontre le mélange adaptatif**.

> Après la démo, mettre `SEED_DEMO=false` dans `backend/.env` pour cesser de
> régénérer les données à chaque démarrage.

---

## 8. Limites & feuille de route

- **Modèle** : mélange heuristique (a priori + réel normalisé), pas encore un
  modèle ML entraîné — volontairement, le temps que l'historique s'accumule
  (`confiance` monte automatiquement avec les données).
- **Gare = ville de départ** : on agrège par `villeDepart` (toujours présent).
  Étape suivante : distinguer plusieurs gares physiques d'une même ville via `codeGareDepart`.
- **Capacité réelle** : le signal réel se base sur le nombre de tickets, pas encore
  sur un taux de remplissage (capacité initiale du trajet non stockée).
- **Suivant** : temps réel (places libérées, retards), notifications « départ imminent »,
  et un vrai modèle prédictif quand l'historique est suffisant.

---

## 9. Récapitulatif des fichiers

**Backend — créés**
`dto/CreneauAffluenceDto.java`, `dto/AffluenceGareDto.java`,
`dto/LigneHeatmapDto.java`, `dto/AffluenceCompagnieDto.java`,
`service/AffluenceService.java`, `controller/AffluenceController.java`,
`config/DemoAffluenceSeeder.java`

**Backend — modifiés**
`repository/ReservationRepository.java`, `controller/CompagnieController.java`,
`config/SecurityConfig.java`, `resources/application.yml` (`app.seed-demo`)

**Frontend — créés**
`core/models/affluence.model.ts`, `core/services/affluence.service.ts`

**Frontend — modifiés**
`features/recherche/resultats.component.{ts,html,scss}`,
`features/espace-compagnie/espace-compagnie.component.{ts,html,scss}`

> Correctif connexe : `model/CompagnieProfile.java` — colonnes `logo_url`,
> `image_couverture_url` et images de galerie passées en `text` (illimité) pour
> stocker les images base64 sans erreur de longueur au démarrage.
