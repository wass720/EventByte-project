package fr.soft_labs.eventbyte.booking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.soft_labs.eventbyte.booking.application.ConfirmBookingPaymentService.Outcome;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfirmBookingPaymentServiceTest {

    private final InMemoryBookingRepository repository = new InMemoryBookingRepository();
    private final FakePaymentGateway payments = new FakePaymentGateway();
    private final RecordingBookingNotifier notifier = new RecordingBookingNotifier();
    private ConfirmBookingPaymentService service;

    @BeforeEach
    void setUp() {
        service = new ConfirmBookingPaymentService(repository, payments, notifier);
    }

    private Booking awaitingPayment(String id) {
        Booking booking = Booking.request(id, "user-1", "evt-1", Instant.parse("2026-01-01T10:00:00Z"),
                        Duration.ofMinutes(10), 30, List.of(new BookingLine("t-1", "Early Bird", 2, 2500)))
                .reserve()
                .awaitPayment(FakePaymentGateway.paymentOf(id));
        repository.save(booking);
        return booking;
    }

    @Test
    void a_successful_payment_confirms_the_booking_and_notifies_its_owner() {
        awaitingPayment("b-1");

        assertEquals(Outcome.CONFIRMED, service.paymentSucceeded("pi_b-1"));

        assertEquals(BookingStatus.CONFIRMED, repository.store.get("b-1").status());
        assertEquals(List.of(BookingStatus.CONFIRMED), notifier.statuses());
        assertEquals("user-1", notifier.notified.get(0).userId());
        assertTrue(payments.refunded.isEmpty());
    }

    @Test
    void the_same_event_delivered_twice_confirms_once_and_never_refunds() {
        awaitingPayment("b-1");

        service.paymentSucceeded("pi_b-1");
        assertEquals(Outcome.ALREADY_CONFIRMED, service.paymentSucceeded("pi_b-1"));

        assertEquals(BookingStatus.CONFIRMED, repository.store.get("b-1").status());
        assertEquals(1, notifier.notified.size());
        assertTrue(payments.refunded.isEmpty());
    }

    @Test
    void a_payment_arriving_after_expiration_is_refunded_and_the_booking_stays_expired() {
        awaitingPayment("b-1");
        repository.markExpired("b-1");

        assertEquals(Outcome.REFUNDED, service.paymentSucceeded("pi_b-1"));

        assertEquals(BookingStatus.EXPIRED, repository.store.get("b-1").status());
        assertEquals(List.of("pi_b-1"), payments.refunded);
        assertTrue(notifier.notified.isEmpty());
    }

    @Test
    void a_booking_expiring_during_the_confirmation_is_refunded_instead() {
        awaitingPayment("b-1");
        // L'expiration passe entre la lecture et l'écriture du service.
        InMemoryBookingRepository racing = new InMemoryBookingRepository() {
            private boolean expiredOnce;

            @Override
            public boolean update(Booking booking, BookingStatus expectedStatus) {
                if (!expiredOnce) {
                    expiredOnce = true;
                    markExpired(booking.id());
                }
                return super.update(booking, expectedStatus);
            }
        };
        racing.store.putAll(repository.store);
        service = new ConfirmBookingPaymentService(racing, payments, notifier);

        assertEquals(Outcome.REFUNDED, service.paymentSucceeded("pi_b-1"));

        assertEquals(BookingStatus.EXPIRED, racing.store.get("b-1").status());
        assertEquals(List.of("pi_b-1"), payments.refunded);
    }

    @Test
    void an_unknown_payment_is_ignored() {
        assertEquals(Outcome.UNKNOWN_PAYMENT, service.paymentSucceeded("pi_inconnu"));

        assertTrue(payments.refunded.isEmpty());
        assertTrue(notifier.notified.isEmpty());
    }

    @Test
    void a_refund_failure_propagates_so_that_stripe_retries_the_event() {
        awaitingPayment("b-1");
        repository.markExpired("b-1");
        payments.failOnRefund = true;

        assertThrows(PaymentUnavailableException.class, () -> service.paymentSucceeded("pi_b-1"));
    }
}
