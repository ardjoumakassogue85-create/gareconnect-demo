# Anti-file d'attente — les 3 briques IA

> Suite de [anti-file-attente.md](anti-file-attente.md). On passe ici de la
> **prédiction** d'affluence à **trois usages concrets de l'IA** qui aident
> vraiment à éviter la file : un assistant conversationnel, une file virtuelle
> qui étale les arrivées, et un enrichissement par le contexte du monde réel.

## Philosophie : cœur déterministe + IA en surcouche sûre

Règle d'or de toute l'implémentation : **l'IA ne doit jamais casser l'application
ni inventer de fausses données.**

- Un **cœur déterministe** (scoring, optimisation) calcule toujours un résultat
  correct à partir des vraies données.
- L'**IA (Gemini)** ne fait qu'une surcouche : elle **choisit parmi des options
  réelles**, **reformule** en langage humain, ou **apporte du contexte externe**.
- Chaque appel IA passe par un [`GeminiClient`](../backend/src/main/java/com/hackathon/gares/service/GeminiClient.java)
  défensif : clé absente, timeout ou erreur → `Optional.empty()` → **repli
  automatique** sur le déterministe. La démo fonctionne même sans réseau.

Savoir **où l'IA aide vraiment** (langage, contexte) et où un algorithme suffit
(prédire un nombre, optimiser une file) est un choix d'ingénierie assumé.

---

## 1. Assistant conversationnel « anti-file »

**Ce que c'est.** Le voyageur pose sa question en langage naturel — ou ne dit rien
et laisse l'assistant proposer — et reçoit un **plan clair** : quel départ prendre,
à quelle heure arriver, et pourquoi ça évite la file.

**Pourquoi c'est important.** C'est le levier n°1 de l'étalement de la demande :
en orientant chaque voyageur vers un créneau plus calme, on aplatit le pic. Et
c'est démontrable en live — l'effet « waouh » du jury.

**Architecture.**
1. **Extraction** des critères (villes, date, heure, intention) via l'assistant IA
   existant, avec repli heuristique local.
2. **Cœur déterministe** — [`AssistantAntiFileService`](../backend/src/main/java/com/hackathon/gares/service/AssistantAntiFileService.java)
   classe les départs disponibles par **« coût de file »** =
   `affluence prévue à l'heure du départ + 0,25 × écart à l'heure souhaitée`.
3. **Surcouche IA** — Gemini reçoit ces départs (déjà triés) et **choisit** celui
   qui minimise l'attente, puis **rédige** un message chaleureux. Son choix est
   **validé** contre la liste réelle : un id inconnu ⇒ on garde le n°1 déterministe.
4. **Repli** — IA indisponible ⇒ message déterministe (« Prends le départ de X,
   arrive vers Y… »).

**API.** `POST /api/assistant/anti-file` (public)
`{ texteLibre?, villeDepart?, villeArrivee?, date? }` →
`{ message, resume, trajetRecommande, heureArrivee, niveauAffluence, alternatives, source }`.
Le champ `source` (`IA` | `DETERMINISTE`) affiche en toute transparence l'origine.

**Exemple réel (testé).** Pour Abidjan → Korhogo, l'IA a répondu :
> *« Optez pour le départ de 20:30… affluence très faible de 28%… arrivez vers
> 20:10… c'est le meilleur créneau pour contourner la file tout en faisant des
> économies ! »* — elle a choisi le départ le plus calme **et** le moins cher.

**Côté UX.** Après chaque recherche, une carte **« 🤖 Assistant anti-file »** se
remplit automatiquement : message, départ recommandé mis en avant (ruban vert),
heure d'arrivée, alternatives cliquables, et un champ pour préciser en langage
naturel (« le moins cher », « avant 8h »).

---

## 2. File virtuelle « coupe-file »

**Ce que c'est.** Le voyageur prend un **créneau d'arrivée** de 15 min. Le système
lui attribue la fenêtre **la moins chargée** avant son départ.

**Pourquoi c'est important.** C'est le passage de *informer* à **agir**. Au lieu de
laisser tout le monde arriver à 07h00, on **répartit activement** les arrivées :
le pic est écrêté, la file raccourcit pour tous. C'est le mécanisme anti-file le
plus direct — et le vrai différenciateur.

**Où est l'« IA ».** Honnêteté assumée : ce n'est **pas** du LLM, c'est de
l'**optimisation**. [`FileVirtuelleService`](../backend/src/main/java/com/hackathon/gares/service/FileVirtuelleService.java)
choisit la fenêtre qui minimise la **charge** =
`affluence prévue à cette heure + (voyageurs déjà placés × 8)`.
Plus une fenêtre se remplit, plus elle « coûte » cher, donc les suivants sont
poussés ailleurs → étalement automatique.

**API.** `POST /api/file-virtuelle` (rôle CLIENT) `{ trajetId }` →
`{ gare, dateVoyage, heureDepart, fenetreDebut, fenetreFin, position, message, dejaAttribue }`.
Idempotent : un même voyageur garde son créneau. Persisté en base (table
`creneaux_arrivee`), donc l'occupation des fenêtres est partagée entre voyageurs.

**Exemple réel (testé).** Pour un départ de 07:30 (FORTE), le service a attribué
la fenêtre **06:30–06:45** (la plus calme), position 1 :
> *« Présente-toi à la gare d'Abidjan entre 06:30 et 06:45. Tu es le 1er sur ce
> créneau : en venant à cette heure, tu évites la file du départ de 07:30. »*

**Côté UX.** Sur le départ recommandé, un bouton **« 🎟️ Prendre mon créneau
coupe-file »** affiche un **pass** (fenêtre + position + consigne claire).

---

## 3. Enrichissement contextuel par l'IA

**Ce que c'est.** Pour une gare et une date, l'IA détecte un **jour férié, une
fête, un grand marché ou un événement** qui gonfle ou réduit l'affluence, et
renvoie un **facteur** + une **explication**.

**Pourquoi c'est important.** Un modèle numérique ne « sait » pas que le 7 août est
la fête nationale. C'est **le vrai apport du LLM** : injecter le **contexte du
monde réel** que la donnée seule n'a pas. Concrètement, ça prévient le voyageur
d'arriver plus tôt les jours exceptionnels.

**Architecture.** [`ContexteAffluenceService`](../backend/src/main/java/com/hackathon/gares/service/ContexteAffluenceService.java)
interroge Gemini, borne le facteur à `[0.5 ; 2.0]`, **met en cache** par
ville+date (pas de rappel inutile), et **retombe neutre** (facteur 1.0, rien
d'affiché) si l'IA est indisponible.

**API.** `GET /api/affluence/contexte?ville=&date=` (public) →
`{ facteur, raison, actif }`. `actif` vaut vrai seulement si le contexte modifie
nettement l'affluence — sinon aucune bannière ne s'affiche.

**Exemples réels (testés).**
- **7 août** → `facteur 1.7` : *« Fête nationale de l'Indépendance… long week-end
  propice aux déplacements massifs vers l'intérieur »*.
- **25 décembre** → `facteur 0.7` : *« la majorité des déplacements ont lieu les
  jours précédents »* — un raisonnement fin qu'aucun modèle numérique ne produirait.

**Côté UX.** Une bannière d'alerte en tête des résultats : *« ⚠️ Affluence
exceptionnelle prévue (+70%) — Fête nationale… Pense à arriver plus tôt. »*, avec
un petit badge **via IA**. Chargée en asynchrone : elle n'attend jamais l'IA pour
afficher le reste.

---

## Résilience & configuration

- **Clé IA** : `GEMINI_API_KEY` dans `backend/.env` (modèle `gemini-flash-latest`).
- **Sans clé / hors-ligne** : les 3 fonctionnalités marchent en mode déterministe
  (F1 : scoring ; F2 : optimisation, inchangée ; F4 : neutre, pas de bannière).
- **Timeouts** courts côté client (8–15 s) et côté `GeminiClient` (6 s connect /
  12 s read) pour ne jamais figer l'UI.

## Récapitulatif des fichiers

**Backend — créés**
`service/GeminiClient.java` · `service/AssistantAntiFileService.java` ·
`controller/AssistantAntiFileController.java` ·
`dto/ConseilAntiFileRequest.java` · `dto/ConseilAntiFileResponse.java` ·
`model/CreneauArrivee.java` · `repository/CreneauArriveeRepository.java` ·
`service/FileVirtuelleService.java` · `controller/FileVirtuelleController.java` ·
`dto/CreneauArriveeRequest.java` · `dto/CreneauArriveeResponse.java` ·
`service/ContexteAffluenceService.java` · `dto/ContexteAffluenceDto.java`

**Backend — modifiés**
`controller/AffluenceController.java` (endpoint `/contexte`) ·
`config/SecurityConfig.java` (`/api/assistant/**`, `/api/file-virtuelle/**`)

**Frontend — créés**
`core/models/assistant.model.ts` · `core/services/assistant.service.ts`

**Frontend — modifiés**
`core/models/affluence.model.ts` · `core/services/affluence.service.ts` ·
`features/recherche/resultats.component.{ts,html,scss}`

## Le pitch jury

> *« Notre IA ne prédit pas juste la foule. Elle **négocie** avec chaque voyageur le
> meilleur moment pour venir (assistant), **répartit** activement les arrivées pour
> casser le pic (file virtuelle), et **comprend le monde réel** — un jour férié, un
> match — que la donnée seule ignore. Et si l'IA tombe, tout continue de marcher. »*
