package fr.soft_labs.eventbyte.booking.infrastructure.payment;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.HasId;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import fr.soft_labs.eventbyte.booking.application.ConfirmBookingPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-17 : Stripe nous informe du résultat des paiements (webhook).
 *
 * Route publique (Stripe n'a pas de JWT) : l'authenticité vient de la signature
 * "Stripe-Signature", vérifiée avec le secret du webhook sur le corps BRUT de la requête.
 * Sans signature valide : 400, rien n'est traité.
 *
 * - payment_intent.succeeded : confirme la réservation (ou rembourse, voir
 *   ConfirmBookingPaymentService).
 * - payment_intent.payment_failed : rien à faire. La réservation reste "awaiting-payment" et
 *   l'acheteur peut réessayer avec une autre carte jusqu'à l'expiration.
 * - Autres événements : ignorés (200, pour que Stripe ne les renvoie pas).
 *
 * Une erreur de traitement répond 500 : Stripe renverra l'événement plus tard (jusqu'à 3 jours).
 *
 * En local : stripe listen --forward-to localhost:8080/payments/stripe/webhook
 */
@RestController
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final ConfirmBookingPaymentService confirmBookingPaymentService;
    private final StripeProperties properties;

    public StripeWebhookController(ConfirmBookingPaymentService confirmBookingPaymentService,
                                   StripeProperties properties) {
        this.confirmBookingPaymentService = confirmBookingPaymentService;
        this.properties = properties;
    }

    @PostMapping("/payments/stripe/webhook")
    public ResponseEntity<Void> handle(@RequestBody String payload,
                                       @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (!properties.hasWebhookSecret()) {
            log.error("Webhook Stripe reçu mais eventbyte.stripe.webhook-secret (STRIPE_WEBHOOK_SECRET) est absent");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, properties.webhookSecret());
        } catch (SignatureVerificationException | RuntimeException e) {
            log.warn("Webhook Stripe refusé : signature invalide ou corps illisible");
            return ResponseEntity.badRequest().build();
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            String paymentIntentId;
            try {
                paymentIntentId = objectId(event);
            } catch (EventDataObjectDeserializationException | ClassCastException e) {
                log.error("Événement Stripe {} illisible", event.getId(), e);
                return ResponseEntity.badRequest().build();
            }
            try {
                ConfirmBookingPaymentService.Outcome outcome =
                        confirmBookingPaymentService.paymentSucceeded(paymentIntentId);
                log.info("Paiement {} réussi (événement {}) : {}", paymentIntentId, event.getId(), outcome);
            } catch (RuntimeException e) {
                log.error("Traitement du paiement {} impossible (événement {}) : Stripe réessaiera",
                        paymentIntentId, event.getId(), e);
                return ResponseEntity.internalServerError().build();
            }
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Id du PaymentIntent de l'événement. getObject() est vide si la version d'API du webhook
     * diffère de celle du SDK : on désérialise alors quand même (l'"id" ne change jamais).
     */
    private static String objectId(Event event) throws EventDataObjectDeserializationException {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject object = deserializer.getObject().isPresent()
                ? deserializer.getObject().get()
                : deserializer.deserializeUnsafe();
        return ((HasId) object).getId();
    }
}
