# GareConnect

Plateforme web de reservation pour les gares interurbaines. L'application aide les voyageurs a rechercher un trajet, comparer les offres, reserver un billet et gerer leurs reservations. Elle permet aussi aux compagnies de transport de publier leurs trajets et de disposer d'une vitrine publique.

## Fonctionnalites principales

- Authentification avec roles client et compagnie
- Recherche de trajets par ville de depart, ville d'arrivee et date
- Reservation avec paiement simule
- Ticket avec QR code
- Annulation possible pendant 30 minutes apres reservation
- Espace client avec billets a venir, passes, annules et tous les billets
- Espace compagnie pour gerer les trajets et les informations de la compagnie
- Vitrine publique par compagnie
- Backend Spring Boot securise par JWT
- Frontend Angular responsive et compatible PWA

## Stack technique

- Frontend : Angular 18
- Backend : Spring Boot 3, Spring Security, JWT
- Base de donnees : PostgreSQL en production, H2 possible en local
- Hebergement prevu :
  - GitHub pour le code source
  - Vercel pour le frontend Angular
  - Render pour le backend Spring Boot

## Structure du projet

```text
.
+-- backend/      # API Spring Boot
+-- frontend/     # Application Angular
+-- README.md
```

## Lancement en local

### 1. Backend

Depuis le dossier `backend` :

```bash
mvn spring-boot:run
```

Par defaut, l'API demarre sur :

```text
http://localhost:8080/api
```

Par defaut, aucun mot de passe de base de donnees n'est necessaire : le backend utilise H2 en memoire pour le developpement local.

Pour utiliser PostgreSQL/Supabase, copier le fichier d'exemple :

```bash
cp backend/.env.example backend/.env
```

Puis remplir les variables :

```env
SUPABASE_DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
SUPABASE_DB_USER=<utilisateur_supabase>
SUPABASE_DB_PASSWORD=<mot_de_passe_prive>
JWT_SECRET=<chaine_secrete_longue>
JWT_EXPIRATION_MS=86400000
ALLOWED_ORIGINS=http://localhost:4200
```

Ne jamais mettre le vrai mot de passe dans le code, le README ou GitHub. Les valeurs avec `<...>` sont publiques parce qu'elles sont fausses : elles servent seulement d'exemple. Le vrai mot de passe reste dans `backend/.env`, qui est ignore par Git.

### 2. Frontend

Depuis le dossier `frontend` :

```bash
npm install
npm start
```

L'application Angular demarre sur :

```text
http://localhost:4200
```

En local, le frontend appelle le backend via :

```ts
apiUrl: 'http://localhost:8080/api'
```

## Build

### Frontend

```bash
cd frontend
npm run build
```

Le build est genere dans :

```text
frontend/dist/frontend
```

### Backend

```bash
cd backend
mvn clean package
```

## Deploiement

### 1. GitHub

Le depot GitHub doit contenir les dossiers `frontend` et `backend`.

Ne pas envoyer les secrets :

- `backend/.env`
- `frontend/node_modules`
- `frontend/dist`
- `backend/target`

Ces fichiers sont deja ignores par `.gitignore`.

### 2. Backend sur Render

Creer un service Web Render connecte au depot GitHub.

Configuration conseillee :

```text
Root Directory: backend
Build Command: mvn clean package -DskipTests
Start Command: java -jar target/gares-0.0.1-SNAPSHOT.jar
```

Variables d'environnement a ajouter dans Render :

```env
SUPABASE_DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
SUPABASE_DB_USER=<utilisateur_supabase>
SUPABASE_DB_PASSWORD=<mot_de_passe_prive>
JWT_SECRET=<chaine_secrete_longue>
JWT_EXPIRATION_MS=86400000
ALLOWED_ORIGINS=https://URL-DU-FRONTEND-VERCEL
```

Ces variables Render sont privees : elles ne doivent pas etre ecrites dans le depot GitHub.

Render donnera ensuite une URL publique du type :

```text
https://nom-du-backend.onrender.com
```

L'API sera donc :

```text
https://nom-du-backend.onrender.com/api
```

### 3. Frontend sur Vercel

Creer un projet Vercel connecte au meme depot GitHub.

Configuration conseillee :

```text
Root Directory: frontend
Framework Preset: Angular
Build Command: npm run build
Output Directory: dist/frontend/browser
```

Avant le deploiement final, remplacer l'URL de production dans :

```text
frontend/src/environments/environment.prod.ts
```

Par l'URL Render du backend :

```ts
export const environment = {
  production: true,
  apiUrl: 'https://nom-du-backend.onrender.com/api',
};
```

Apres modification, pousser le code sur GitHub. Vercel redeploiera automatiquement le frontend.

## Ordre recommande pour mettre en ligne

1. Pousser le projet complet sur GitHub
2. Deployer le backend Spring Boot sur Render
3. Recuperer l'URL publique Render
4. Mettre cette URL dans `frontend/src/environments/environment.prod.ts`
5. Deployer le frontend Angular sur Vercel
6. Mettre l'URL Vercel dans `ALLOWED_ORIGINS` sur Render
7. Redeployer le backend si necessaire

## Notes importantes

- GitHub heberge le code source, mais pas le backend Spring Boot en execution.
- Vercel est utilise pour le frontend Angular.
- Render est utilise pour l'API Spring Boot.
- Les reservations deja creees gardent leurs donnees en base. Les changements de duree d'annulation s'appliquent aux nouvelles reservations.
- Un mot de passe de base de donnees ne doit jamais etre public. Si un vrai mot de passe a deja ete partage, il faut le regenerer dans Supabase puis mettre la nouvelle valeur uniquement dans `backend/.env` et dans Render.
