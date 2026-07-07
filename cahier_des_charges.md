# Cahier des charges — Plateforme de gares interurbaines intelligente

## 1. Problème réel identifié

Dans les gares de transport interurbain, les voyageurs font face à :
- Files d'attente longues et imprévisibles, sans visibilité sur l'affluence
- Absence de comparaison simple entre gares/compagnies (prix, horaires, fiabilité)
- Manque d'information officielle et à jour côté compagnies, en particulier pour les compagnies peu connues qui n'ont pas de présence en ligne crédible

Côté compagnies de transport :
- Faible visibilité/image de marque, surtout pour les compagnies non connues
- Pas d'outil simple pour centraliser et communiquer leurs informations (gares, horaires, tarifs)
- Aucune donnée sur l'affluence prévisible pour anticiper les ressources

## 2. Utilisateurs cibles

| Profil | Besoin principal |
|---|---|
| Voyageur | Trouver rapidement le meilleur trajet (gare, horaire, tarif) et réserver |
| Compagnie de transport | Gagner en visibilité, présenter ses gares/services, gérer son offre |

## 3. Proposition de valeur

Une plateforme qui centralise l'offre de plusieurs compagnies de transport interurbain, aide le voyageur à choisir via un moteur de recommandation assisté par IA, et donne à chaque compagnie une vitrine professionnelle pour améliorer son image — le tout avec réservation en ligne.

## 4. Périmètre du MVP (4 jours, 1 développeur)

### Inclus
- **Authentification** : compte client et compte compagnie (rôles distincts), Spring Security + JWT
- **Espace voyageur** :
  - Recherche structurée (ville départ, ville arrivée, date, budget)
  - Recommandation enrichie par IA : interprétation d'une requête en langage libre + reformulation de la recommandation (LLM en surcouche d'un moteur de scoring déterministe)
  - Réservation avec paiement simulé (traçable, table dédiée)
- **Espace compagnie** :
  - Formulaire de saisie : logo, description, informations de gare, trajets, tarifs
  - Gestion des trajets (CRUD)
- **Vitrine publique** :
  - Page dynamique par gare (template unique, alimenté par les données saisies par la compagnie)
- **Collecte de données d'usage** : chaque recherche/réservation est loguée (base pour une future prédiction d'affluence)
- **PWA** : installable, manifest, service worker

### Explicitement exclu du MVP (roadmap)
- Modèle prédictif d'affluence entraîné sur données réelles (pas assez d'historique en 4 jours — la collecte démarre, le modèle viendra après)
- Paiement réel (Stripe/Mobile Money en production)
- Multi-langue
- Notifications push/SMS

## 5. Exigences non fonctionnelles
- Application déployée et accessible publiquement dès J1 (Vercel + Railway), pas seulement en local
- Responsive mobile-first (usage réel probable majoritairement mobile)
- Résilience démo : si l'appel LLM échoue ou est lent, fallback automatique sur le formulaire structuré

## 6. Stack technique
- Frontend : Angular 18+ (PWA)
- Backend : Spring Boot, Spring Security, JWT
- Base de données / Storage : Supabase (Postgres + Storage)
- Hébergement : Vercel (frontend), Railway (backend)
- IA : appel API LLM pour extraction de paramètres en langage naturel + reformulation de recommandation

## 7. Critères de succès pour la démo
- Parcours complet démontrable en live : recherche → recommandation IA → réservation → paiement simulé → ticket
- Parcours compagnie démontrable : création de compte → saisie infos gare → vitrine visible publiquement
- Application PWA installable sur mobile pendant la démo
