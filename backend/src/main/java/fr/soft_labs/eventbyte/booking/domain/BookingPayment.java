package fr.soft_labs.eventbyte.booking.domain;

/**
 * Le paiement préparé chez le prestataire (US-16) : son identifiant, et le secret que le
 * navigateur utilise pour payer (Stripe : PaymentIntent id + client_secret).
 *
 * Le secret ne permet que de payer CE montant : il est renvoyé à l'acheteur uniquement.
 */
public record BookingPayment(String id, String clientSecret) {

    public BookingPayment {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Identifiant de paiement manquant");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("Secret client de paiement manquant");
        }
    }
}
