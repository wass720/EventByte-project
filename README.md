# EventByte - Documentation des API

## Installation et Configuration

### Prérequis

Avant de lancer les containers Docker, vous devez configurer les variables d'environnement nécessaires pour Strapi et l'application web.

#### 1. Configuration de Strapi

**Créer un compte Cloudinary:**
- Rendez-vous sur [https://cloudinary.com](https://cloudinary.com) et créez un compte gratuit
- Récupérez vos identifiants : Cloud Name, API Key et API Secret

**Configurer le fichier `.env` pour Strapi:**
- Copiez le fichier `.env.example` vers `.env` dans le dossier `strapi/`
- Ajoutez vos identifiants Cloudinary dans les variables d'environnement correspondantes

**Ajouter une image** avant de run le seeding via le CLI
- Rendez-vous sur [http://localhost:1337/admin/plugins/upload](http://localhost:1337/admin/plugins/upload)
- Cliquer sur `Add new assets`

#### 2. Configuration de l'application Web

**Créer un compte Stripe:**
- Rendez-vous sur [https://stripe.com](https://stripe.com) et créez un compte
- Récupérez votre clé publique (Publishable Key) depuis le dashboard Stripe

**Configurer le fichier `.env` pour Web:**
- Copiez le fichier `.env.example` vers `.env` dans le dossier `web/`
- Ajoutez votre clé publique Stripe dans les variables d'environnement correspondantes

#### 3. Configuration du paiement côté backend (US-16)

> Pas à pas complet, avec la Stripe CLI et le dépannage : **[STRIPE.md](STRIPE.md)**.

Le backend crée les paiements Stripe (PaymentIntents) avec la **clé secrète** du même compte Stripe :
- Récupérez la clé secrète de test (`sk_test_...`) depuis le dashboard Stripe (Developers > API keys)
- Exportez-la avant de lancer le backend : `STRIPE_SECRET_KEY=sk_test_...` (variable d'environnement, ne jamais la committer)

Sans cette variable, le backend démarre mais toute réservation est **rejetée** (le paiement ne peut pas être préparé).

**Webhook Stripe (US-17)** : c'est Stripe qui prévient le backend qu'un paiement a réussi. En local, avec la [Stripe CLI](https://docs.stripe.com/stripe-cli) :
```bash
stripe login
stripe listen --forward-to localhost:8080/payments/stripe/webhook
```
La commande affiche un secret `whsec_...` : exportez-le avant de lancer le backend, `STRIPE_WEBHOOK_SECRET=whsec_...`.
Sans webhook, un paiement réussi n'est jamais confirmé : la réservation reste `awaiting-payment` (et garde ses places).

**Frontend en mode réel** : dans `frontend/web/.env`, `VITE_CONFIG_MODE=production`, `VITE_API_URL=http://localhost:8080`,
et une `VITE_STRIPE_PUBLIC_KEY` (`pk_test_...`) du **même compte Stripe** que la clé secrète.
Carte de test : `4242 4242 4242 4242`, date future, CVC quelconque.

#### 4. Lancer les containers

Une fois la configuration terminée, lancez tous les containers requis avec Docker Compose :

```bash
docker compose up -d
```

Cette commande va démarrer tous les services nécessaires (base de données, Strapi, etc.) en arrière-plan.

---

# Strapi

Strapi envoie sur RabbitMQ dans l'exchange events les messages suivants: 

- `event.created`
- `event.updated`
- `event.deleted`

et tout les messages on comme data l'Event :

```ts
type Event = {
    id: string;
    title: string;
    description: string;
    cover: string;
    venue: {
        name: string;
        address: string;
        latitude: number;
        longitude: number;
    },
    tickets: {
        id: string;
        name: string;
        quantity: number;
        price: number;  // en EUROS (décimal, tel que saisi dans Strapi) : le backend le convertit en centimes
    }[]
}
```


---

# Connections aux services

Pour Keycloak et RabbitMQ, l'identifiant et le mot de passe sont `admin`.

---

## Table des matières
1. [API Événements](#api-événements)
2. [API Likes](#api-likes)
3. [API Tickets](#api-tickets)
4. [API Réservations](#api-réservations)
5. [Authentification](#authentification)

---

## API Événements

### 1. Récupérer les événements à découvrir
**Route:** `GET /events/discover`

**Query Parameters:**
```typescript
{
  lat: number,    // Latitude de l'utilisateur
  lng: number     // Longitude de l'utilisateur
}
```

**Réponse:** `200 OK`
```typescript
Array<{
  id: string,
  title: string,
  cover: string,              // URL de l'image de couverture
  likedCount: number,         // Nombre de likes
  venue: {
    name: string,
    adresse: string,
    coordinates: {
      latitude: number,
      longitude: number
    }
  },
  isSoldOut: boolean,         // Indique si l'événement est complet
  startAt: string,            // Date de début (format ISO 8601)
  lowestPrice: number,        // Prix le plus bas en centimes
  description: string
}>
```

---

### 2. Récupérer un événement par son ID
**Route:** `GET /events/:id`

**Paramètres URL:**
- `id`: Identifiant de l'événement

**Réponse:** `200 OK`
```typescript
{
  id: string,
  title: string,
  cover: string,
  likedCount: number,
  venue: {
    name: string,
    adresse: string,
    coordinates: {
      latitude: number,
      longitude: number
    }
  },
  isSoldOut: boolean,
  startAt: string,
  lowestPrice: number,
  description: string
}
```

---

### 3. Récupérer plusieurs événements par leurs IDs
**Route:** `GET /events`

**Query Parameters:**
```typescript
{
  ids: string  // Liste d'IDs séparés par des virgules (ex: "id1,id2,id3")
}
```

**Réponse:** `200 OK`
```typescript
Array<Event>  // Même structure qu'un événement unique
```

---

### 4. Rechercher des événements
**Route:** `GET /events/search`

**Query Parameters:**
```typescript
{
  lat: number,      // Latitude de recherche
  lng: number,      // Longitude de recherche
  radius: number,   // Rayon de recherche en kilomètres
  query: string     // Terme de recherche
}
```

**Réponse:** `200 OK`
```typescript
Array<Event>  // Liste d'événements correspondants
```

---

### 5. Récupérer les événements likés par l'utilisateur
**Route:** `GET /events/liked`

**Headers requis:**
```
Authorization: Bearer <token>
```

**Réponse:** `200 OK`
```typescript
Array<Event>  // Liste des événements likés
```

---

## API Likes

### 1. Liker un événement
**Route:** `POST /events/:eventId/like`

**Paramètres URL:**
- `eventId`: Identifiant de l'événement

**Headers requis:**
```
Authorization: Bearer <token>
```

**Réponse:** `200 OK`
```typescript
void  // Pas de corps de réponse
```

---

### 2. Unliker un événement
**Route:** `DELETE /events/:eventId/like`

**Paramètres URL:**
- `eventId`: Identifiant de l'événement

**Headers requis:**
```
Authorization: Bearer <token>
```

**Réponse:** `200 OK`
```typescript
void  // Pas de corps de réponse
```

---

## API Tickets

### 1. Récupérer les tickets d'un événement
**Route:** `GET /events/:eventId/tickets`

**Paramètres URL:**
- `eventId`: Identifiant de l'événement

**Réponse:** `200 OK`
```typescript
Array<{
  id: string,
  eventId: string,
  name: string,      // Nom du type de ticket (ex: "Early Bird", "VIP")
  price: number,     // Prix en centimes
  quantity: number   // Quantité disponible
}>
```

---

## API Réservations

### 1. Créer une réservation
**Route:** `POST /bookings`

**Headers requis:**
```
Authorization: Bearer <token>
```

**Corps de la requête:**
```typescript
{
  items: Array<{
    id: string,        // ID du ticket
    quantity: number   // Quantité à réserver
  }>
}
```

**Réponse:** `200 OK`
```typescript
{
  id: string  // ID de la réservation créée
}
```

---

### 2. Récupérer une réservation par son ID
**Route:** `GET /bookings/:id`

**Paramètres URL:**
- `id`: Identifiant de la réservation

**Headers requis:**
```
Authorization: Bearer <token>
```

**Réponse:** `200 OK`
```typescript
{
  id: string,
  eventId: string,
  bookedAt: string,           // Date de réservation (format ISO 8601)
  lines: Array<{
    id: string,
    name: string,             // Nom du ticket
    quantity: number,
    price: number             // Prix unitaire en centimes
  }>,
  status: 'requested' | 'reserved' | 'rejected' | 'awaiting-payment' | 'expired' | 'confirmed',
  total: number,              // Total en centimes, frais inclus (= somme des lignes + fees)
  fees: number,               // Frais en centimes (30 centimes par ticket)
  clientSecret?: string       // Secret du PaymentIntent Stripe, présent uniquement en 'awaiting-payment'
}
```

**Réponse:** `404 Not Found`
Si la réservation n'existe pas.

**Paiement (US-16) :** quand les places sont bloquées, le backend crée un PaymentIntent Stripe de `total`
centimes (EUR, carte, sans redirection) et la réservation passe `awaiting-payment` avec son `clientSecret`
avant la réponse de `POST /bookings`. Le frontend paie avec `stripe.confirmPayment`. Si Stripe refuse
ou ne répond pas, la réservation passe `rejected` et ses places sont rendues. À l'expiration (US-14),
le PaymentIntent est annulé avant de rendre les places ; s'il ne peut pas l'être (déjà payé, Stripe
indisponible), la réservation garde ses places et l'expiration est retentée au passage suivant.
`POST /bookings` répond `400` si le total est sous le minimum payable par carte (0,50 €).

---

### 3. Récupérer toutes les réservations de l'utilisateur
**Route:** `GET /bookings/user`

**Headers requis:**
```
Authorization: Bearer <token>
```

**Réponse:** `200 OK`
```typescript
Array<Booking>  // Liste de réservations (même structure que ci-dessus)
```

---

### 4. Stream des mises à jour de réservation (SSE)
**Route:** `GET /bookings/stream`

**Headers requis:**
```
Authorization: Bearer <token>
```

**Type:** Server-Sent Events (SSE)

**Événements envoyés:**

**Événement:** `booking_updated`
```typescript
{
  id: string,
  eventId: string,
  bookedAt: string,
  lines: Array<{
    id: string,
    name: string,
    quantity: number,
    price: number
  }>,
  status: 'requested' | 'reserved' | 'rejected' | 'awaiting-payment' | 'expired' | 'confirmed',
  total: number,
  fees: number,
  clientSecret?: string
}
```

**Note:** Ce endpoint utilise Server-Sent Events (SSE) pour envoyer en temps réel les mises à jour de statut des réservations à l'utilisateur connecté.

**Fonctionnement (US-18) :**
- Un événement `booking_updated` est envoyé à **chaque changement de statut** (`reserved`, `rejected`,
  `awaiting-payment`, `expired`, `confirmed`), avec le même JSON que `GET /bookings/:id`.
- On ne reçoit **que ses propres réservations** : le flux est lié à l'utilisateur du JWT.
- Un commentaire `ping` est envoyé toutes les 25 s pour garder la connexion ouverte. Le serveur ferme le flux
  au bout de 30 min ; le navigateur se reconnecte automatiquement (le front relit alors un token à jour).
- Limite connue : les connexions sont gardées en mémoire, donc une seule instance du backend est supportée.

---

### 5. Webhook de paiement Stripe (US-17)
**Route:** `POST /payments/stripe/webhook`

Appelée par Stripe, pas par le frontend. Pas de JWT : la requête est authentifiée par son en-tête
`Stripe-Signature`, vérifié avec `STRIPE_WEBHOOK_SECRET`.

**Réponses :** `200` (traité ou ignoré), `400` (signature invalide), `500` (erreur : Stripe renverra
l'événement), `503` (secret du webhook non configuré).

| Événement Stripe | Effet |
|---|---|
| `payment_intent.succeeded` | La réservation `awaiting-payment` passe `confirmed` (+ `booking_updated` sur le flux SSE). Un événement reçu deux fois ne fait rien de plus. |
| `payment_intent.payment_failed` | Rien : la réservation reste `awaiting-payment`, l'utilisateur peut réessayer avec une autre carte jusqu'à l'expiration. |
| autres | Ignorés. |

**Paiement arrivant après l'expiration (décision) :** le paiement est **remboursé intégralement et
automatiquement**, et la réservation reste `expired`. Les places ont déjà été remises en vente, donc
confirmer risquerait de vendre deux fois la même place. Ce cas est rare : avant de faire expirer une
réservation, le backend annule son paiement Stripe ; si l'annulation échoue (paiement déjà effectué),
la réservation n'expire pas et sera confirmée par le webhook.

---

## Authentification

Tous les endpoints marqués avec "Headers requis" nécessitent un token d'authentification JWT dans le header `Authorization`.

**Format du header:**
```
Authorization: Bearer <votre_token_jwt>
```

Le token doit être obtenu via votre système d'authentification (ex: Keycloak).

---

## Notes importantes

### Statuts de réservation
1. **requested** : Réservation demandée
2. **reserved** : Tickets réservés temporairement
3. **rejected** : Réservation refusée (manque de disponibilité)
4. **awaiting-payment** : En attente de paiement
5. **expired** : Réservation expirée
6. **confirmed** : Réservation confirmée après paiement

---

# Backend

## Configuration dependencies

```gradle
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-amqp'
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    implementation 'com.meilisearch.sdk:meilisearch-java:0.12.0'
}
```

## application.yml

```yaml
spring:
  application:
    name: eventbyte
  datasource:
    url: jdbc:postgresql://localhost:5432/eventbyte
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: validate
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin
  data:
    redis:
      host: localhost
      port: 6379
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/eventbyte

meilisearch:
  host: http://localhost:7700
  api-key: Cd9_wZ5drjqixClHEnbof-4h0bgktUzXwaY3yt8wJoM

```

## Security

```java
package fr.soft_labs.eventbyte.shared.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));

        return http.build();
    }
}
