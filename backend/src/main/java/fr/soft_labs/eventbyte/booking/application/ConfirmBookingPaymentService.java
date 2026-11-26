package fr.soft_labs.eventbyte.booking.application;

import fr.soft_labs.eventbyte.booking.application.port.BookingNotifier;
import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.application.port.PaymentGateway;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * US-17 : Stripe nous informe (webhook) qu'un paiement a réussi.
 *
 * - Réservation "awaiting-payment" : elle passe "confirmed" (compare-and-set : si l'expiration
 *   passe en même temps, une seule des deux transitions gagne).
 * - Déjà "confirmed" : rien à faire. Stripe peut livrer le même événement plusieurs fois.
 * - DÉCISION : paiement arrivé APRÈS l'expiration (ou pour une réservation rejetée). Les places
 *   ont déjà été rendues et peut-être revendues : on ne peut pas confirmer sans risquer de
 *   vendre deux fois. Le paiement est donc remboursé intégralement et automatiquement, et la
 *   réservation reste "expired". Ce cas est rare : l'expiration annule d'abord le paiement
 *   Stripe (US-16), et une réservation dont le paiement n'a pas pu être annulé n'expire pas.
 *
 * Une erreur (base, Stripe) remonte : le webhook répond alors 500 et Stripe réessaie.
 */
@Service
public class ConfirmBookingPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmBookingPaymentService.class);

    private final BookingRepository bookingRepository;
    private final PaymentGateway paymentGateway;
    private final BookingNotifier bookingNotifier;

    public ConfirmBookingPaymentService(BookingRepository bookingRepository,
                                        PaymentGateway paymentGateway,
                                        BookingNotifier bookingNotifier) {
        this.bookingRepository = bookingRepository;
        this.paymentGateway = paymentGateway;
        this.bookingNotifier = bookingNotifier;
    }

    public Outcome paymentSucceeded(String paymentId) {
        // Deux essais : si la réservation change entre la lecture et l'écriture, on relit
        // pour traiter son nouveau statut (en pratique : elle vient d'expirer -> remboursement).
        for (int attempt = 0; attempt < 2; attempt++) {
            Optional<Booking> found = bookingRepository.findByPaymentId(paymentId);
            if (found.isEmpty()) {
                log.warn("Paiement {} réussi mais aucune réservation ne lui correspond : ignoré", paymentId);
                return Outcome.UNKNOWN_PAYMENT;
            }
            Booking booking = found.get();
            switch (booking.status()) {
                case CONFIRMED -> {
                    return Outcome.ALREADY_CONFIRMED;
                }
                case AWAITING_PAYMENT -> {
                    Booking confirmed = booking.confirm();
                    if (bookingRepository.update(confirmed, BookingStatus.AWAITING_PAYMENT)) {
                        bookingNotifier.bookingChanged(confirmed);
                        return Outcome.CONFIRMED;
                    }
                    // Le statut a changé entre-temps : on relit.
                }
                default -> {
                    log.warn("Paiement {} reçu pour la réservation {} au statut {} : remboursement",
                            paymentId, booking.id(), booking.status().value());
                    paymentGateway.refundPayment(paymentId);
                    return Outcome.REFUNDED;
                }
            }
        }
        throw new IllegalStateException("La réservation du paiement " + paymentId + " change sans arrêt");
    }

    public enum Outcome {
        CONFIRMED,
        ALREADY_CONFIRMED,
        REFUNDED,
        UNKNOWN_PAYMENT
    }
}
