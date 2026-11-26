package fr.soft_labs.eventbyte.booking.application;

import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.application.port.PaymentGateway;
import fr.soft_labs.eventbyte.booking.application.port.StockReserver;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingPayment;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * US-16 : préparer le paiement par carte d'une réservation dont les places sont bloquées.
 *
 * Crée le paiement chez le prestataire (Stripe PaymentIntent, montant = total en centimes),
 * puis passe la réservation "reserved" -> "awaiting-payment" avec son clientSecret : le
 * frontend l'utilise pour payer (GET /bookings/:id).
 *
 * - Prestataire en erreur : la réservation passe "rejected" et ses places sont rendues
 *   (l'acheteur n'a jamais reçu de secret, il ne peut donc rien payer).
 * - Réservation qui a changé entre-temps (expirée par US-14) : le paiement est annulé,
 *   pour que personne ne paie des places déjà rendues.
 * Les deux cas passent par BookingRepository.update (compare-and-set sur "reserved") :
 * une seule transition gagne, les places ne sont jamais rendues deux fois.
 */
@Service
public class StartBookingPaymentService {

    private static final Logger log = LoggerFactory.getLogger(StartBookingPaymentService.class);

    private final PaymentGateway paymentGateway;
    private final BookingRepository bookingRepository;
    private final StockReserver stockReserver;

    public StartBookingPaymentService(PaymentGateway paymentGateway,
                                      BookingRepository bookingRepository,
                                      StockReserver stockReserver) {
        this.paymentGateway = paymentGateway;
        this.bookingRepository = bookingRepository;
        this.stockReserver = stockReserver;
    }

    /** @param reserved une réservation "reserved", déjà enregistrée */
    public void execute(Booking reserved) {
        BookingPayment payment;
        try {
            payment = paymentGateway.createPayment(reserved);
        } catch (RuntimeException e) {
            log.error("Paiement impossible à préparer pour la réservation {} : elle est rejetée",
                    reserved.id(), e);
            if (bookingRepository.update(reserved.rejectPayment(), BookingStatus.RESERVED)) {
                stockReserver.release(reserved);
            }
            return;
        }

        if (!bookingRepository.update(reserved.awaitPayment(payment), BookingStatus.RESERVED)) {
            log.warn("La réservation {} a changé avant la fin de la préparation du paiement : "
                    + "paiement {} annulé", reserved.id(), payment.id());
            cancelQuietly(payment.id());
        }
    }

    private void cancelQuietly(String paymentId) {
        try {
            paymentGateway.cancelPayment(paymentId);
        } catch (RuntimeException e) {
            log.error("Annulation du paiement {} impossible", paymentId, e);
        }
    }
}
