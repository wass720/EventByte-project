package fr.soft_labs.eventbyte.booking.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookingTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static Booking requested() {
        return Booking.request("b-1", "user-1", "evt-1", NOW, Duration.ofMinutes(10), List.of(
                new BookingLine("t-1", "Early Bird", 2, 2500),
                new BookingLine("t-2", "VIP", 1, 9000)));
    }

    @Test
    void a_new_booking_is_requested_and_its_total_sums_the_lines() {
        Booking booking = requested();

        assertEquals(BookingStatus.REQUESTED, booking.status());
        assertEquals(14000, booking.total());
        assertEquals(0, booking.fees());
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
                () -> Booking.request("b-1", "user-1", "evt-1", NOW, Duration.ofMinutes(10), List.of()));
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
