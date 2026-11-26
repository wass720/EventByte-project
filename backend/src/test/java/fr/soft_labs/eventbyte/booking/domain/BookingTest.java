package fr.soft_labs.eventbyte.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookingTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static final int FEE_PER_TICKET = 30;
    private static final BookingPayment PAYMENT = new BookingPayment("pi_1", "pi_1_secret_x");

    private static Booking requested() {
        return Booking.request("b-1", "user-1", "evt-1", NOW, Duration.ofMinutes(10), FEE_PER_TICKET, List.of(
                new BookingLine("t-1", "Early Bird", 2, 2500),
                new BookingLine("t-2", "VIP", 1, 9000)));
    }

    @Test
    void a_new_booking_is_requested_and_its_total_is_the_lines_plus_the_fees() {
        Booking booking = requested();

        assertEquals(BookingStatus.REQUESTED, booking.status());
        assertEquals(90, booking.fees());               // 3 tickets x 30 centimes
        assertEquals(14000 + 90, booking.total());      // 2 x 2500 + 9000, frais inclus
        assertNull(booking.payment());
    }

    @Test
    void fees_can_be_zero_but_not_negative() {
        Booking free = Booking.request("b-1", "u", "evt", NOW, Duration.ofMinutes(10), 0,
                List.of(new BookingLine("t-1", "A", 4, 1000)));
        assertEquals(0, free.fees());
        assertEquals(4000, free.total());

        assertThrows(IllegalArgumentException.class, () -> Booking.request("b-1", "u", "evt", NOW,
                Duration.ofMinutes(10), -1, List.of(new BookingLine("t-1", "A", 1, 1000))));
    }

    @Test
    void an_amount_too_large_for_an_int_is_refused_instead_of_overflowing() {
        assertThrows(ArithmeticException.class, () -> Booking.request("b-1", "u", "evt", NOW,
                Duration.ofMinutes(10), 30, List.of(new BookingLine("t-1", "A", 1, Integer.MAX_VALUE))));
    }

    @Test
    void a_reserved_booking_awaits_its_payment_with_the_client_secret() {
        Booking awaiting = requested().reserve().awaitPayment(PAYMENT);

        assertEquals(BookingStatus.AWAITING_PAYMENT, awaiting.status());
        assertEquals(PAYMENT, awaiting.payment());
        assertEquals(requested().total(), awaiting.total());
        assertEquals(requested().fees(), awaiting.fees());
    }

    @Test
    void only_a_reserved_booking_can_await_a_payment() {
        assertThrows(IllegalStateException.class, () -> requested().awaitPayment(PAYMENT));
        assertThrows(IllegalStateException.class, () -> requested().reject().awaitPayment(PAYMENT));
        assertThrows(IllegalStateException.class,
                () -> requested().reserve().awaitPayment(PAYMENT).awaitPayment(PAYMENT));
        assertThrows(IllegalArgumentException.class, () -> requested().reserve().awaitPayment(null));
    }

    @Test
    void a_reserved_booking_whose_payment_cannot_be_prepared_is_rejected() {
        assertEquals(BookingStatus.REJECTED, requested().reserve().rejectPayment().status());
        assertThrows(IllegalStateException.class, () -> requested().rejectPayment());
        assertThrows(IllegalStateException.class,
                () -> requested().reserve().awaitPayment(PAYMENT).rejectPayment());
    }

    @Test
    void a_booking_awaiting_payment_expires_and_keeps_its_payment_to_cancel_it() {
        Booking awaiting = requested().reserve().awaitPayment(PAYMENT);

        assertTrue(awaiting.isDueForExpiration(NOW.plus(Duration.ofMinutes(10))));
        Booking expired = awaiting.expire();
        assertEquals(BookingStatus.EXPIRED, expired.status());
        assertEquals(PAYMENT, expired.payment());
    }

    @Test
    void awaiting_payment_without_a_payment_is_impossible() {
        Booking b = requested();
        assertThrows(IllegalArgumentException.class, () -> new Booking(b.id(), b.userId(), b.eventId(),
                b.bookedAt(), b.expiresAt(), b.lines(), BookingStatus.AWAITING_PAYMENT, b.total(), b.fees(), null));
    }

    @Test
    void a_payment_needs_an_id_and_a_client_secret() {
        assertThrows(IllegalArgumentException.class, () -> new BookingPayment(null, "s"));
        assertThrows(IllegalArgumentException.class, () -> new BookingPayment(" ", "s"));
        assertThrows(IllegalArgumentException.class, () -> new BookingPayment("pi_1", null));
        assertThrows(IllegalArgumentException.class, () -> new BookingPayment("pi_1", ""));
    }

    @Test
    void reserve_moves_a_requested_booking_to_reserved() {
        assertEquals(BookingStatus.RESERVED, requested().reserve().status());
    }

    @Test
    void reject_moves_a_requested_booking_to_rejected() {
        assertEquals(BookingStatus.REJECTED, requested().reject().status());
    }

    @Test
    void a_decided_booking_cannot_change_status_again() {
        Booking reserved = requested().reserve();

        assertThrows(IllegalStateException.class, reserved::reserve);
        assertThrows(IllegalStateException.class, reserved::reject);
        assertThrows(IllegalStateException.class, () -> requested().reject().reserve());
    }

    @Test
    void a_booking_needs_at_least_one_line() {
        assertThrows(IllegalArgumentException.class,
                () -> Booking.request("b-1", "user-1", "evt-1", NOW, Duration.ofMinutes(10), 30, List.of()));
    }

    @Test
    void a_line_quantity_must_be_positive() {
        assertThrows(IllegalArgumentException.class, () -> new BookingLine("t-1", "Early Bird", 0, 2500));
        assertThrows(IllegalArgumentException.class, () -> new BookingLine("t-1", "Early Bird", -1, 2500));
    }

    @Test
    void status_values_follow_the_api_contract() {
        assertEquals("awaiting-payment", BookingStatus.AWAITING_PAYMENT.value());
        assertEquals(BookingStatus.CONFIRMED, BookingStatus.fromValue("confirmed"));
        assertThrows(IllegalArgumentException.class, () -> BookingStatus.fromValue("nope"));
    }

    @Test
    void the_payment_deadline_is_the_booking_time_plus_the_window() {
        assertEquals(NOW.plus(Duration.ofMinutes(10)), requested().expiresAt());
    }

    @Test
    void a_reserved_booking_expires_once_its_deadline_is_reached() {
        Booking reserved = requested().reserve();

        assertFalse(reserved.isDueForExpiration(NOW.plus(Duration.ofMinutes(10)).minusSeconds(1)));
        assertTrue(reserved.isDueForExpiration(NOW.plus(Duration.ofMinutes(10))));
        assertTrue(reserved.isDueForExpiration(NOW.plus(Duration.ofHours(1))));
    }

    @Test
    void only_unpaid_bookings_can_expire() {
        Instant late = NOW.plus(Duration.ofHours(1));

        assertEquals(BookingStatus.EXPIRED, requested().reserve().expire().status());
        assertFalse(requested().isDueForExpiration(late));            // requested
        assertFalse(requested().reject().isDueForExpiration(late));   // rejected
        assertFalse(requested().reserve().expire().isDueForExpiration(late)); // déjà expirée
        assertThrows(IllegalStateException.class, () -> requested().expire());
        assertThrows(IllegalStateException.class, () -> requested().reject().expire());
        assertThrows(IllegalStateException.class, () -> requested().reserve().expire().expire());
    }
}
