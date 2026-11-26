# Paiement Stripe en local (US-16 à US-18)

Ce guide explique comment faire tourner le paiement par carte sur votre machine. Comptez 10 minutes la première fois.

Le paiement implique trois acteurs :
- **le front** affiche le formulaire de carte, avec la clé **publique** ;
- **le backend** crée le paiement, avec la clé **secrète** ;
- **la Stripe CLI** transmet au backend la confirmation que le paiement a réussi (le webhook).

Il faut les trois. Les deux clés doivent venir du **même compte Stripe**.

---

## 1. Récupérer les clés Stripe

1. Créez un compte sur https://dashboard.stripe.com, ou connectez-vous.
2. Restez en **mode Test** (interrupteur en haut à droite).
3. Dans **Developers → API keys**, copiez :
   - la **Publishable key** `pk_test_...`, pour le front ;
   - la **Secret key** `sk_test_...`, pour le backend. Elle ne doit **jamais** être commitée.

## 2. Installer la Stripe CLI (une seule fois)

```powershell
winget install Stripe.StripeCli
```

Ensuite, **rouvrez le terminal**. Dans VS Code, il faut parfois fermer et rouvrir VS Code entièrement pour que la commande `stripe` soit reconnue.

```powershell
stripe login
```

La commande ouvre le navigateur. Choisissez le même compte qu'à l'étape 1.

## 3. Lancer l'écoute du webhook

Dans un terminal que vous **laissez ouvert** pendant vos tests :

```powershell
stripe listen --events payment_intent.succeeded,payment_intent.payment_failed --forward-to localhost:8080/payments/stripe/webhook
```

La commande affiche :

```
Ready! Your webhook signing secret is whsec_xxxxxxxxxxxx
```

Ce `whsec_...` est le secret du webhook. Il reste le même d'une fois sur l'autre pour votre compte.

## 4. Lancer le backend

Dans un **autre** terminal :

```powershell
cd backend
$env:STRIPE_SECRET_KEY="sk_test_..."
$env:STRIPE_WEBHOOK_SECRET="whsec_..."
./gradlew bootRun
```

Les variables `$env:` ne durent que le temps du terminal : redéfinissez-les à chaque nouveau terminal. Sous macOS/Linux, utilisez `export STRIPE_SECRET_KEY=...`.

Gradle reste affiché sur « EXECUTING » tant que le serveur tourne : c'est normal. Le backend écoute sur http://localhost:8080.

## 5. Configurer le front

Dans `frontend/web/.env` :

```
VITE_CONFIG_MODE=production
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PUBLIC_KEY=pk_test_...
```

Puis reconstruisez le conteneur, car le code du front est copié dans l'image :

```powershell
docker compose up -d --build web
```

## 6. Tester

1. Allez sur http://localhost:5173 et connectez-vous.
2. Réservez des tickets, puis cliquez sur **Pay**.
3. Utilisez la carte de test `4242 4242 4242 4242`, n'importe quelle date future et n'importe quel CVC.
4. La page affiche « Processing », puis passe d'elle-même à **SUCCESS** dès que Stripe a confirmé le paiement.

D'autres cartes de test (refusée, 3D Secure…) sont listées sur https://docs.stripe.com/testing.

---

## Dépannage

| Symptôme | Cause | Solution |
|---|---|---|
| Le bouton **Pay** reste grisé | Le front est en mode `demo`, ou les clés `pk` et `sk` viennent de deux comptes différents | Vérifiez l'étape 5, puis `docker compose up -d --build web` |
| La réservation passe en **Booking rejected** | Le backend n'a pas `STRIPE_SECRET_KEY`, ou la clé est fausse | Cherchez `STRIPE_SECRET_KEY absente` ou `Paiement impossible à préparer` dans les logs du backend, puis relancez-le avec la clé |
| La page reste sur **Processing** après le paiement | `stripe listen` n'est pas lancé, ou `STRIPE_WEBHOOK_SECRET` est absent ou faux | Étapes 3 et 4. Le terminal de `stripe listen` doit afficher `payment_intent.succeeded → [200]` |
| `stripe` : commande introuvable | Le terminal a été ouvert avant l'installation | Rouvrez le terminal ou VS Code |
| `must specify events to forward` | Option `--events` oubliée (versions récentes de la CLI) | Reprenez la commande complète de l'étape 3 |
| Aucun événement à réserver | La base du backend est vide : les événements Strapi ont été publiés pendant que le backend était arrêté | Dans Strapi (http://localhost:1337/admin), ouvrez chaque événement et cliquez sur **Save** |
| `password authentication failed for user "root"` au lancement du backend | Un PostgreSQL installé sur Windows occupe le port 5432 avant Docker | Arrêtez-le (`Stop-Service postgresql-x64-18`, dans un PowerShell administrateur), ou voir ci-dessous |

### Si un PostgreSQL Windows occupe le port 5432

Si vous ne voulez pas arrêter ce service, exposez aussi le Postgres Docker sur le port 5434. Créez `docker-compose.override.yml` à la racine du projet. Ce fichier ne concerne que votre machine : **ne le commitez pas**.

```yaml
services:
  postgres:
    ports:
      - "5434:5432"
```

Puis :

```powershell
docker compose up -d postgres
cd backend
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5434/eventbyte"
./gradlew bootRun
```
