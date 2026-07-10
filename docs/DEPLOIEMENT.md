# Déploiement de GareConnect (version démo)

Objectif : mettre **cette version** en ligne pour vérifier que toutes les nouvelles
fonctionnalités marchent, **sans toucher au vrai repo GitHub** (`Vibeathon`).

## Architecture cible

```
  Navigateur
      │  https
      ▼
  Frontend Angular  ──►  Backend Spring Boot  ──►  Base PostgreSQL
     (Vercel)              (Railway, Docker)          (Supabase)
```

- **Frontend** (Angular) → **Vercel** (hébergement statique, HTTPS gratuit)
- **Backend** (Spring Boot) → **Railway** (build via le `Dockerfile` fourni)
- **Base de données** → **Supabase** (déjà en place, on réutilise la même)

> ⚠️ Les secrets (mots de passe Supabase, clé JWT, clé Gemini, mot de passe SMTP) ne
> vont **jamais** dans le repo. Ils se saisissent dans le tableau de bord Railway.
> Le fichier `backend/.env` reste ignoré par git (déjà le cas).

---

## Étape 0 — Créer un NOUVEAU repo GitHub (isolé du vrai)

Le vrai repo (`origin`) reste **intact** : on ne pousse jamais dessus. On crée un
dépôt séparé et on y pousse une branche dédiée.

**A. Créer le dépôt vide sur GitHub**

- Soit en ligne : github.com → New repository → nom `gareconnect-demo` → *Private* → Create.
- Soit en ligne de commande (si `gh` est installé et connecté) :
  ```bash
  gh repo create gareconnect-demo --private --source=. --remote=deploy --push=false
  ```

**B. Committer cette version sur une branche dédiée et la pousser sur le NOUVEAU repo**

Depuis `projet-hackathon/projet-hackathon/` :
```bash
git checkout -b deploy
git add -A
git commit -m "Version demo: anti-file IA + billet signe + page A propos + config deploiement"

# Ajouter le NOUVEAU repo comme remote (remplace l'URL par la tienne)
git remote add deploy https://github.com/<ton-compte>/gareconnect-demo.git

# Pousser la branche 'deploy' comme 'main' du NOUVEAU repo — origin (Vibeathon) n'est PAS touché
git push -u deploy deploy:main
```

> `origin` (Vibeathon) n'est jamais utilisé ici : aucune de ces commandes n'écrit dessus.

---

## Étape 1 — Backend sur Railway

1. Va sur **railway.app** → *New Project* → *Deploy from GitHub repo* → choisis
   `gareconnect-demo`.
2. Railway détecte plusieurs dossiers : ouvre les **Settings** du service et mets
   **Root Directory = `backend`**. Il utilisera automatiquement le `Dockerfile`.
3. Onglet **Variables** → ajoute les variables ci-dessous (copie les valeurs depuis
   ton `backend/.env` local ; **ne mets pas** de guillemets) :

   | Variable | Valeur | D'où ça vient |
   |---|---|---|
   | `SUPABASE_DB_URL` | `jdbc:postgresql://…supabase…` | `backend/.env` |
   | `SUPABASE_DB_USER` | (ton user Supabase) | `backend/.env` |
   | `SUPABASE_DB_PASSWORD` | (ton mot de passe Supabase) | `backend/.env` |
   | `JWT_SECRET` | chaîne ≥ 32 caractères | `backend/.env` |
   | `JWT_EXPIRATION_MS` | `86400000` | `backend/.env` |
   | `GEMINI_API_KEY` | (ta clé Gemini) | `backend/.env` |
   | `SMTP_HOST` | `smtp.gmail.com` | `backend/.env` |
   | `SMTP_PORT` | `587` | `backend/.env` |
   | `SMTP_USERNAME` | (ton e-mail d'envoi) | `backend/.env` |
   | `SMTP_PASSWORD` | (mot de passe d'application Gmail) | `backend/.env` |
   | `MAIL_FROM` | (ton e-mail d'envoi) | `backend/.env` |
   | `SEED_DEMO` | `true` | pour peupler la démo (données variées) |
   | `ALLOWED_ORIGINS` | *(à remplir à l'étape 3)* | URL Vercel |
   | `FRONTEND_BASE_URL` | *(à remplir à l'étape 3)* | URL Vercel |
   | `BILLET_PRIVATE_KEY` | *(optionnel — voir note)* | stabilité des QR |
   | `MAKE_RECLAMATION_WEBHOOK_URL` | *(optionnel)* | webhook réclamations |

   - **Ne définis PAS `PORT`** : Railway l'injecte tout seul, et l'app le lit
     (`server.port: ${PORT}`).
4. Déploie. Quand c'est vert, ouvre **Settings → Networking → Generate Domain** pour
   obtenir l'URL publique, du type `https://gareconnect-demo-production.up.railway.app`.
5. Vérifie que le back répond :
   `https://<ton-back>.up.railway.app/api/affluence/gare?ville=Abidjan` → doit renvoyer du JSON.

---

## Étape 2 — Frontend sur Vercel

1. **Avant de déployer**, pointe le front vers ton backend Railway. Édite
   `frontend/src/environments/environment.prod.ts` :
   ```ts
   export const environment = {
     production: true,
     apiUrl: 'https://<ton-back>.up.railway.app/api',
   };
   ```
   Puis commit + push sur le repo de déploiement :
   ```bash
   git add frontend/src/environments/environment.prod.ts
   git commit -m "Front: pointer vers le backend Railway"
   git push deploy deploy:main
   ```
2. Va sur **vercel.com** → *Add New… → Project* → importe `gareconnect-demo`.
3. Dans la config d'import :
   - **Root Directory = `frontend`**
   - Framework : *Angular* (détecté). Le fichier `frontend/vercel.json` fixe déjà la
     commande de build, le dossier de sortie (`dist/frontend/browser`) et la
     réécriture SPA (pour que `/a-propos`, `/recherche`, etc. marchent au rafraîchissement).
4. Déploie. Tu obtiens une URL du type `https://gareconnect-demo.vercel.app`.

---

## Étape 3 — Rebrancher le CORS (indispensable)

Le backend refuse par défaut les origines inconnues. Il faut l'autoriser à parler à Vercel.

1. Retour sur **Railway → Variables**, renseigne (avec ton URL Vercel exacte, sans `/` final) :
   - `ALLOWED_ORIGINS` = `https://gareconnect-demo.vercel.app`
   - `FRONTEND_BASE_URL` = `https://gareconnect-demo.vercel.app`
   *(tu peux mettre plusieurs origines séparées par des virgules, ex. pour garder localhost)*
2. Railway redéploie automatiquement.
3. Ouvre l'URL Vercel, connecte-toi avec le compte démo :
   `demo-affluence@gareconnect.local` / `DemoPass123!` → tout doit fonctionner, **caméra
   comprise** (Vercel = vrai HTTPS de confiance, donc le scanner marche sur téléphone
   sans aucun bidouillage).

---

## Récapitulatif de l'ordre

1. Créer le repo `gareconnect-demo` + pousser la branche `deploy`.
2. Railway : root `backend`, variables (sauf les 2 URLs Vercel), déployer, récupérer l'URL back.
3. Éditer `environment.prod.ts` avec l'URL back, push.
4. Vercel : root `frontend`, déployer, récupérer l'URL front.
5. Railway : renseigner `ALLOWED_ORIGINS` + `FRONTEND_BASE_URL` avec l'URL front.
6. Tester sur l'URL Vercel (desktop **et** téléphone).

## Backend sur Fly.io (au lieu de Railway) — pour garder le SMTP Gmail

⚠️ **Railway bloque le SMTP sortant** : la vérification e-mail à l'inscription ne
fonctionne pas depuis Railway. **Fly.io autorise le SMTP (port 587)** → ton Gmail
marche tel quel, sans service tiers. On déploie donc le **backend sur Fly.io** ; le
frontend reste sur Vercel et la base sur Supabase.

Le `backend/Dockerfile` et `backend/fly.toml` sont déjà prêts. Depuis le dossier
`backend/` :

```bash
# 1. Installer flyctl (PowerShell Windows)
pwsh -Command "iwr https://fly.io/install.ps1 -useb | iex"

# 2. Se connecter (ouvre le navigateur ; carte bancaire requise par Fly)
fly auth login

# 3. Créer l'app à partir du fly.toml existant, sans déployer tout de suite
fly launch --no-deploy --copy-config
#   -> si le nom "gareconnect-demo" est pris, Fly en proposera un autre : accepte.

# 4. Définir TOUS les secrets (copie les valeurs depuis backend/.env)
fly secrets set \
  SUPABASE_DB_URL="..." SUPABASE_DB_USER="..." SUPABASE_DB_PASSWORD="..." \
  JWT_SECRET="..." JWT_EXPIRATION_MS="86400000" GEMINI_API_KEY="..." \
  SMTP_HOST="smtp.gmail.com" SMTP_PORT="587" SMTP_USERNAME="..." SMTP_PASSWORD="..." \
  MAIL_FROM="..." SEED_DEMO="true" \
  ALLOWED_ORIGINS="https://<ton-front>.vercel.app" \
  FRONTEND_BASE_URL="https://<ton-front>.vercel.app"

# 5. Déployer
fly deploy
```

- L'app écoute sur **8081** (déjà réglé dans `fly.toml` : `internal_port = 8081`).
- URL publique : `https://<nom-app>.fly.dev`. Teste :
  `https://<nom-app>.fly.dev/api/affluence/gare?ville=Abidjan` → JSON attendu.
- Mets cette URL `.fly.dev/api` dans `frontend/src/environments/environment.prod.ts`,
  commit + push, et Vercel se redéploie.

## Notes

- **`SEED_DEMO=true`** : au premier démarrage, le backend peuple des données de démo
  variées (affluence sur toutes les lignes, réservations, notifications). Une fois la
  démo validée, tu peux repasser à `false`.
- **`BILLET_PRIVATE_KEY`** : si non défini, une clé de signature des billets est
  regénérée à chaque redémarrage → les anciens QR ne se valident plus après un redeploy.
  Pour une démo stable, génère une clé RSA (PKCS8 base64) une fois et fixe-la ici.
- **Base partagée** : Railway utilise la **même** base Supabase que ton local. Les
  comptes/réservations créés en ligne apparaîtront aussi en local (et inversement).
- **Sécurité** : aucun secret n'est dans le repo. Si un secret a déjà fuité dans
  l'historique git (ex. ancien webhook), fais-le tourner.
