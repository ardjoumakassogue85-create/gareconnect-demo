# AGENT.md — Plateforme de gares interurbaines (Hackathon, 4 jours)

## Contexte
Voir `cahier_des_charges.md` à la racine pour le périmètre fonctionnel complet.
Développeur : solo. Délai : 4 jours. Priorité absolue : un parcours de bout en bout
qui fonctionne en démo, plutôt que des fonctionnalités isolées inachevées.

## Stack imposée
- Frontend : Angular 18+ (PWA)
- Backend : Spring Boot 3.3, Java 17, Maven, Spring Security, JWT
- DB / Storage : Supabase (Postgres + Storage), utilisé UNIQUEMENT comme base de données
  et stockage fichiers — PAS Supabase Auth. L'authentification est entièrement gérée
  par Spring Security + JWT maison.
- Hébergement : Vercel (frontend), Railway (backend)
- IA : appel API Claude pour extraction de paramètres en langage naturel (recherche)
  et reformulation de la recommandation. Doit avoir un fallback sur le formulaire
  structuré si l'appel échoue ou est lent — ne jamais bloquer le parcours utilisateur
  dessus.

## Règles pour Codex / tout agent qui reprend ce projet
1. Ne jamais introduire Supabase Auth — l'auth passe exclusivement par
   `AuthController` / `JwtUtil` côté Spring Boot.
2. Ne jamais coder un vrai module de paiement externe (Stripe, Mobile Money réel).
   Le paiement est simulé mais TRACÉ dans la table `paiements` avec un statut réaliste.
3. Toute fonctionnalité liée à la "prédiction d'affluence par ML" est hors périmètre
   du MVP. Se limiter à logger les événements d'usage dans `evenements_usage`.
4. Une seule phase à la fois. Ne pas anticiper le code d'une phase suivante avant
   que la phase en cours soit validée manuellement.
5. Toujours fournir des fichiers complets (pas de diffs partiels) sauf demande explicite.
6. Prioriser un parcours fonctionnel de bout en bout à une fonctionnalité isolée
   parfaite mais déconnectée du reste.

## Modèle de données (référence)
Voir `cahier_des_charges.md` section 4/6. Tables : `users`, `compagnies`, `gares`,
`trajets`, `reservations`, `paiements`, `evenements_usage`.

## Phases

### Phase 1 — Fondations backend (EN COURS)
- Structure Maven Spring Boot
- Connexion Supabase Postgres (JPA)
- Entités `User` (+ enum `Role`: CLIENT, COMPAGNIE)
- Auth : inscription / connexion, hash password (BCrypt), génération/validation JWT
- Sécurité : filtre JWT, config des routes publiques vs protégées

### Phase 2 — Backend métier
- Entités `Compagnie`, `Gare`, `Trajet`, `Reservation`, `Paiement`, `EvenementUsage`
- Endpoints CRUD gares/trajets (protégés, rôle COMPAGNIE)
- Endpoint recherche de trajets (public, avec filtres ville/date/budget)
- Endpoint réservation + paiement simulé (protégé, rôle CLIENT)
- Upload logo/image vers Supabase Storage

### Phase 3 — Intégration IA
- Endpoint qui reçoit une requête en langage libre
- Appel API Claude : extraction JSON structuré (ville départ, ville arrivée, date,
  créneau horaire, budget)
- Passage des paramètres extraits au moteur de scoring existant (Phase 2)
- Reformulation en langage naturel de la recommandation retenue
- Fallback strict si l'API échoue : basculer sur la recherche structurée sans erreur
  visible pour l'utilisateur

### Phase 4 — Frontend Angular (PWA)
- Setup Angular 18+, manifest PWA, service worker
- Auth (login/register client + compagnie), guards, intercepteur JWT
- Espace voyageur : recherche (structurée + langage libre), résultats, réservation
- Espace compagnie : formulaire gare/trajets, upload logo
- Vitrine publique `/gare/:id` (template unique, données dynamiques)

### Phase 5 — Déploiement & finitions
- Déploiement Railway (backend) + Vercel (frontend) — variables d'environnement
- Seed de données de démo (villes de Côte d'Ivoire : Abidjan, Bouaké, Yamoussoukro,
  San-Pédro, Korhogo...)
- Test du parcours complet
- Vérification PWA installable

## État d'avancement
- [ ] Phase 1
- [ ] Phase 2
- [ ] Phase 3
- [ ] Phase 4
- [ ] Phase 5
