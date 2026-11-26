# Tester EventByte de bout en bout (front en production, Stripe en mode test)

Ce guide repart **de zéro** : il supprime l'ancien environnement (conteneurs, données, anciens réglages) pour éviter les conflits, puis remonte tout et teste le parcours complet.

Comptez **30 minutes** la première fois. Toutes les commandes sont pour **PowerShell**, lancées depuis la racine du projet, sauf indication contraire.

---

## 0. Prérequis

- **Docker Desktop** démarré
- **Java 21** (`java -version`)
- Un compte **Cloudinary** (gratuit), pour les images de Strapi
- Un compte **Stripe** en **mode test**
- La **Stripe CLI** : `winget install Stripe.StripeCli`, puis fermez et rouvrez le terminal (dans VS Code, redémarrez VS Code)

---

## 1. Tout nettoyer

> ⚠️ Cette étape **supprime toutes les données locales** du projet : événements, comptes Strapi et Keycloak, réservations, likes. C'est voulu : on repart propre.

### 1.1 Arrêter le backend

Dans le terminal du backend : `Ctrl + C`. S'il en reste un en arrière-plan :

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | % { Stop-Process -Id $_.OwningProcess -Force }
```

### 1.2 Arrêter les PostgreSQL installés sur Windows

Ils prennent le port 5432 et le backend se connecte alors à la mauvaise base (`password authentication failed for user "root"`). Dans un **PowerShell administrateur** :

```powershell
Get-Service postgresql* | Stop-Service -Force
Get-Service postgresql* | Set-Service -StartupType Manual
```

Si la commande ne trouve rien, aucun PostgreSQL Windows n'est installé : passez à la suite.

### 1.3 Supprimer les conteneurs et les données du projet

```powershell
docker compose down -v --remove-orphans
```

`-v` supprime aussi les volumes : les bases Postgres et Keycloak, Redis, RabbitMQ et Meilisearch repartent vides.

### 1.4 Supprimer les conteneurs d'autres copies du projet

Si vous avez une autre copie d'EventByte sur votre machine, ses conteneurs utilisent les mêmes ports, et le même nom `keycloak`. Listez ce qui tourne :

```powershell
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Si vous voyez des conteneurs qui ne commencent pas par `eventbyte-project-` (par exemple `keycloak`, ou `eventbyte-postgres-1`), allez dans le dossier de cette autre copie et lancez `docker compose down`. Ou supprimez-les directement :

```powershell
docker rm -f keycloak
```

### 1.5 Supprimer l'ancienne configuration du CLI de seed

Le token enregistré pointait vers l'ancien Strapi, qui n'existe plus :

```powershell
Remove-Item -Recurse -Force .eventbyte -ErrorAction SilentlyContinue
```

---

## 2. Récupérer le code à jour

```powershell
git checkout main
git pull
```

---

## 3. Configurer les fichiers `.env`

Ces deux fichiers sont ignorés par git : **chacun crée les siens**, avec ses propres clés.

### 3.1 Strapi : `frontend/strapi/.env`

```powershell
Copy-Item frontend/strapi/.env.example frontend/strapi/.env
```

Puis complétez avec vos identifiants Cloudinary (Dashboard Cloudinary, page d'accueil) :

```
CLOUDINARY_NAME=votre_cloud_name     ← le "Cloud name" (ex. dxk3abc12), PAS le nom de la clé ("Root")
CLOUDINARY_KEY=votre_api_key
CLOUDINARY_SECRET=votre_api_secret
```

### 3.2 Front : `frontend/web/.env`

```powershell
Copy-Item frontend/web/.env.example frontend/web/.env
```

Puis modifiez ces lignes :

```
VITE_CONFIG_MODE=production
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PUBLIC_KEY=pk_test_...
```

- La clé `pk_test_...` se trouve dans le Dashboard Stripe, **mode test**, **Developers → API keys → Publishable key**.
- ⚠️ **Jamais la clé secrète `sk_test_...` ici** : tout ce qui commence par `VITE_` est envoyé au navigateur. Si vous la mettez par erreur, Stripe.js affiche « You should not use your secret key with Stripe.js ».
- Laissez `VITE_STRIPE_CLIENT_SECRET` tel quel : il ne sert qu'en mode démo.

---

## 4. Lancer l'infrastructure

```powershell
docker compose up -d --build
docker compose ps
```

- `--build` est **obligatoire** : `web` et `strapi` sont des images construites. Sans lui, l'ancien code du front ou de Strapi continue de tourner. Relancez cette commande **après chaque `git pull`** qui touche au front ou à Strapi.
- Les 8 services doivent être `Up` : postgres, redis, rabbitmq, keycloak, keycloak-db, meilisearch, strapi, web.
- Keycloak met environ une minute à démarrer, Strapi un peu plus la première fois.

---

## 5. Lancer Stripe et le backend

Il faut **deux terminaux**, à laisser ouverts pendant tous les tests.

### Terminal A : écoute des webhooks Stripe

```powershell
stripe login
stripe listen --events payment_intent.succeeded,payment_intent.payment_failed --forward-to localhost:8080/payments/stripe/webhook
```

`stripe login` n'est à faire qu'une fois : il ouvre le navigateur, choisissez **le même compte** que pour la clé `pk_test_`. `stripe listen` affiche `Ready! Your webhook signing secret is whsec_...` : **copiez ce `whsec_...`**.

### Terminal B : backend

```powershell
cd backend
$env:STRIPE_SECRET_KEY="sk_test_..."
$env:STRIPE_WEBHOOK_SECRET="whsec_..."
.\gradlew.bat bootRun
```

- `sk_test_...` : Dashboard Stripe, **Developers → API keys → Secret key**.
- `whsec_...` : celui affiché par le terminal A. Mettez le **vrai** secret, pas le texte d'exemple.
- Ces variables ne valent que pour ce terminal : redéfinissez-les à chaque nouveau terminal.
- Attendez `Started EventbyteApplication`. Le message `STRIPE_SECRET_KEY absente` ne doit **pas** apparaître.

> Le backend doit tourner **avant** le seed (étape 6) : c'est lui qui crée la file RabbitMQ. Sinon, les événements publiés par Strapi sont perdus.

---

## 6. Préparer Strapi et générer des événements

1. Ouvrez http://localhost:1337/admin et **créez le compte administrateur**. Il est local : mettez ce que vous voulez.
2. **Media Library → Add new assets** : envoyez une image. Le seed s'en sert comme couverture. Si l'upload donne une erreur 500, vérifiez `CLOUDINARY_NAME` (étape 3.1), puis `docker compose up -d --force-recreate strapi`.
3. **Settings → API Tokens → Create new API Token** : nom `cli`, durée `Unlimited`, type **Full access**. Copiez le token, il ne s'affiche qu'une fois.
4. Dans un troisième terminal, à la racine du projet :

```powershell
.\eventbyte.exe login
.\eventbyte.exe seed -c 50
```

Collez le token à la commande `login`. Dans le terminal B, vous devez voir défiler des lignes `[event.created] traité`.

---

## 7. Créer un compte utilisateur

Ouvrez http://localhost:5173, cliquez sur **Login / Sign up**, puis sur le lien d'inscription de la page Keycloak (l'inscription est ouverte). Le realm utilise **l'email comme identifiant**.

Pour tester l'isolation entre utilisateurs, créez **deux comptes** : un dans une fenêtre normale, un dans une fenêtre de **navigation privée**.

---

## 8. Le parcours de test

Rechargez le front avec **`Ctrl + F5`** pour vider le cache. Cochez chaque ligne :

| # | Action | Résultat attendu |
|---|---|---|
| 1 | Page d'accueil, ville **Paris** | Une liste d'événements, avec date, prix et image. Essayez aussi d'autres villes : le seed répartit les événements dans toute la France. |
| 2 | Dans Strapi, créez un événement à Paris (titre « Test E2E », 2 tickets) et **Save** | Il apparaît sur la page d'accueil après `Ctrl + F5` |
| 3 | Page **Search Events** : mot-clé « E2E », autour de Paris | L'événement est trouvé |
| 4 | Sans être connecté, ouvrez un événement | Le détail et les tickets s'affichent : ces routes sont publiques |
| 5 | Connecté, cliquez sur **Interested** | Le cœur se remplit, le compteur de likes passe à +1 |
| 6 | Cliquez de nouveau sur **Interested** | Le like est retiré, le compteur redescend |
| 7 | Avec le second compte, ouvrez le même événement | Il ne voit pas les likes du premier comme les siens |
| 8 | Réservez 2 tickets | Page de réservation, avec un compte à rebours de 10 minutes |
| 9 | Dans un autre onglet, rouvrez l'événement | La quantité disponible de ce ticket a baissé de 2 |
| 10 | Payez avec `4242 4242 4242 4242`, date `12/34`, CVC `123` | Paiement accepté |
| 11 | **Sans recharger** | Le statut passe tout seul à **Confirmed**. Le terminal A affiche `payment_intent.succeeded --> [200]` |
| 12 | Page **Tickets** | La réservation confirmée apparaît |
| 13 | Nouvelle réservation, payez avec la carte refusée `4000 0000 0000 0002` | Le paiement est refusé, la réservation reste en attente de paiement |
| 14 | Avec le second compte, page **Tickets** | Il ne voit **aucune** réservation du premier compte |

### Tests bonus

- **Le dernier ticket** : dans Strapi, créez un ticket à **1** place. Les deux comptes le réservent presque en même temps. **Une seule** réservation passe, l'autre est refusée.
- **L'expiration** : relancez le backend (terminal B) avec `$env:EVENTBYTE_BOOKING_PAYMENTWINDOW="1m"` avant `.\gradlew.bat bootRun`. Réservez sans payer : au bout d'une minute, la réservation expire en direct et la quantité de tickets remonte.
- **Les tests automatisés** : dans `backend`, lancez `.\gradlew.bat test`. Tout doit être vert. Il faut que Docker tourne, car certains tests utilisent le vrai Postgres et le vrai Redis.

---

## 9. Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| Backend : `password authentication failed for user "root"` | Un PostgreSQL Windows occupe le port 5432 | Étape 1.2 |
| Backend : `Port 8080 was already in use` | Un ancien backend tourne encore | Étape 1.1 |
| `docker compose up` : *container name "/keycloak" is already in use* | Conteneur d'une autre copie du projet | Étape 1.4 |
| Upload Strapi : erreur 500 | Mauvais `CLOUDINARY_NAME` | Étape 3.1, puis `docker compose up -d --force-recreate strapi` |
| Seed réussi, mais aucun événement dans le backend | Backend lancé **après** le seed | Lancer le backend, puis relancer le seed |
| Page d'accueil vide | Backend arrêté, ou pas d'événement près de la ville choisie | Vérifier le terminal B ; essayer une autre ville |
| Le front ressemble à une ancienne version | Image `web` non reconstruite | `docker compose up -d --build web`, puis `Ctrl + F5` |
| Console : « You should not use your secret key with Stripe.js » | `sk_test_` mise dans `VITE_STRIPE_PUBLIC_KEY` | Mettre la `pk_test_` (étape 3.2), puis `docker compose restart web` |
| Réservation refusée tout de suite | `STRIPE_SECRET_KEY` absente | Terminal B, étape 5 |
| Payé, mais reste en attente | `stripe listen` fermé, ou mauvais `whsec_` | Relancer le terminal A ; vérifier que le `whsec_` est le même que dans le terminal B |
| `stripe` n'est pas reconnu | Terminal ouvert avant l'installation | Redémarrer VS Code, ou lancer : `$env:Path = [Environment]::GetEnvironmentVariable('Path','User') + ';' + [Environment]::GetEnvironmentVariable('Path','Machine')` |
| `401` sur `/bookings/user` dans la console au chargement | Appel fait avant que la connexion soit prête | Normal si la page fonctionne une fois connecté |
