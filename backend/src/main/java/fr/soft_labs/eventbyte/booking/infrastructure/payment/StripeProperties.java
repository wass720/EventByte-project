package fr.soft_labs.eventbyte.booking.infrastructure.payment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * eventbyte.stripe.* (US-16).
 *
 * secret-key : clé secrète Stripe (sk_test_... en dev), lue depuis STRIPE_SECRET_KEY.
 *              Ne jamais la committer. Sans elle, toute réservation est rejetée au moment
 *              de préparer le paiement.
 * currency   : devise des paiements (code ISO en minuscules), "eur" comme le frontend.
 * api-base   : URL de l'API Stripe ; ne se change que pour les tests.
 * timeout    : délai max d'un appel à Stripe (la création de réservation l'attend).
 */
@ConfigurationProperties(prefix = "eventbyte.stripe")
public record StripeProperties(String secretKey,
                               @DefaultValue("eur") String currency,
                               @DefaultValue("https://api.stripe.com") String apiBase,
                               @DefaultValue("10s") Duration timeout) {

    public boolean hasSecretKey() {
        return secretKey != null && !secretKey.isBlank();
    }
}
