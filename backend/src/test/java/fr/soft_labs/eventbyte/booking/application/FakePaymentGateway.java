package fr.soft_labs.eventbyte.booking.application;

import fr.soft_labs.eventbyte.booking.application.port.PaymentGateway;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingPayment;
import java.util.ArrayList;
import java.util.List;

/** Faux prestataire de paiement pour les tests de use cases. */
class FakePaymentGateway implements PaymentGateway {

    final List<Booking> created = new ArrayList<>();
    final List<String> cancelled = new ArrayList<>();
    boolean failOnCreate;
    boolean failOnCancel;
    /** Appelé juste avant de renvoyer le paiement : simule ce qui se passe pendant l'appel réseau. */
    Runnable duringCreate = () -> { };
    /** Appelé au début de l'annulation : permet d'observer l'état à ce moment-là. */
    Runnable duringCancel = () -> { };

    @Override
    public BookingPayment createPayment(Booking booking) {
        if (failOnCreate) {
            throw new PaymentUnavailableException("Stripe indisponible");
        }
        created.add(booking);
        duringCreate.run();
        return paymentOf(booking.id());
    }

    @Override
    public void cancelPayment(String paymentId) {
        duringCancel.run();
        if (failOnCancel) {
            throw new PaymentUnavailableException("Stripe indisponible");
        }
        cancelled.add(paymentId);
    }

    static BookingPayment paymentOf(String bookingId) {
        return new BookingPayment("pi_" + bookingId, "pi_" + bookingId + "_secret_x");
    }
}
