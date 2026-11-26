package fr.soft_labs.eventbyte.booking.infrastructure.payment;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentCreateParams.AutomaticPaymentMethods;
import com.stripe.param.PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects;
import com.stripe.param.RefundCreateParams;
import fr.soft_labs.eventbyte.booking.application.PaymentUnavailableException;
import fr.soft_labs.eventbyte.booking.application.port.PaymentGateway;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingPayment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * US-16 : adapter Stripe du port PaymentGateway (API PaymentIntents).
 *
 * - Montant = booking.total, en centimes : c'est l'unité attendue par Stripe pour l'euro.
 * - Moyens de paiement automatiques SANS redirection : le frontend appelle
 *   stripe.confirmPayment({ redirect: 'if_required' }) sans return_url, donc seuls les
 *   moyens sans redirection (la carte) doivent être proposés.
 * - Clé d'idempotence dérivée de l'id de réservation : un nouvel essai après une coupure
 *   réseau renvoie le même PaymentIntent au lieu d'en créer un second.
 * - Métadonnées booking_id / user_id / event_id : visibles dans le dashboard, et utiles au
 *   webhook de confirmation (US-17).
 */
@Component
public class StripePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);

    private final StripeProperties properties;
    private final StripeClient client;

    public StripePaymentGateway(StripeProperties properties) {
        this.properties = properties;
        if (!properties.hasSecretKey()) {
            log.warn("eventbyte.stripe.secret-key (STRIPE_SECRET_KEY) absente : "
                    + "les réservations seront rejetées faute de pouvoir préparer le paiement");
            this.client = null;
            return;
        }
        int timeoutMillis = Math.toIntExact(properties.timeout().toMillis());
        this.client = StripeClient.builder()
                .setApiKey(properties.secretKey())
                .setApiBase(properties.apiBase())
                .setConnectTimeout(timeoutMillis)
                .setReadTimeout(timeoutMillis)
                .build();
    }

    @Override
    public BookingPayment createPayment(Booking booking) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) booking.total())
                .setCurrency(properties.currency())
                .setAutomaticPaymentMethods(AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(AllowRedirects.NEVER)
                        .build())
                .setDescription("EventByte - réservation " + booking.id())
                .putMetadata("booking_id", booking.id())
                .putMetadata("user_id", booking.userId())
                .putMetadata("event_id", booking.eventId())
                .build();
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("booking-" + booking.id() + "-payment")
                .build();
        try {
            PaymentIntent intent = client().v1().paymentIntents().create(params, options);
            return new BookingPayment(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new PaymentUnavailableException(
                    "Stripe a refusé la création du paiement : " + e.getMessage(), e);
        }
    }

    /**
     * Idempotent : un paiement déjà annulé (par un passage précédent) est un succès.
     * Un paiement déjà réussi ou en cours ne peut pas être annulé : PaymentUnavailableException.
     */
    @Override
    public void cancelPayment(String paymentId) {
        StripeClient stripe = client();
        try {
            stripe.v1().paymentIntents().cancel(paymentId);
        } catch (StripeException e) {
            if (isAlreadyCanceled(stripe, paymentId)) {
                return;
            }
            throw new PaymentUnavailableException(
                    "Stripe a refusé l'annulation du paiement " + paymentId + " : " + e.getMessage(), e);
        }
    }

    /** Remboursement intégral. Clé d'idempotence par paiement : jamais deux remboursements. */
    @Override
    public void refundPayment(String paymentId) {
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("refund-" + paymentId)
                .build();
        try {
            client().v1().refunds().create(
                    RefundCreateParams.builder().setPaymentIntent(paymentId).build(), options);
        } catch (StripeException e) {
            throw new PaymentUnavailableException(
                    "Stripe a refusé le remboursement du paiement " + paymentId + " : " + e.getMessage(), e);
        }
    }

    private static boolean isAlreadyCanceled(StripeClient stripe, String paymentId) {
        try {
            return "canceled".equals(stripe.v1().paymentIntents().retrieve(paymentId).getStatus());
        } catch (StripeException e) {
            return false;
        }
    }

    private StripeClient client() {
        if (client == null) {
            throw new PaymentUnavailableException("Clé secrète Stripe non configurée (STRIPE_SECRET_KEY)");
        }
        return client;
    }
}
