package fr.soft_labs.eventbyte.booking.application.port;

import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingPayment;

/**
 * Port sortant : le prestataire de paiement par carte (Stripe en production).
 *
 * Les erreurs du prestataire sont levées en PaymentUnavailableException.
 */
public interface PaymentGateway {

    /**
     * Prépare le paiement de "booking.total" (centimes). Idempotent pour une même réservation :
     * rappeler la méthode ne crée pas un second paiement.
     */
    BookingPayment createPayment(Booking booking);

    /** Annule un paiement préparé et pas encore payé : il ne pourra plus aboutir. */
    void cancelPayment(String paymentId);

    /**
     * US-17 : rembourse intégralement un paiement reçu. Idempotent : rappeler la méthode pour
     * le même paiement ne rembourse pas deux fois.
     */
    void refundPayment(String paymentId);
}
