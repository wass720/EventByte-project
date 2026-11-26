package fr.soft_labs.eventbyte.booking.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.soft_labs.eventbyte.booking.application.port.StockReserver;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartBookingPaymentServiceTest {

    private final InMemoryBookingRepository repository = new InMemoryBookingRepository();
    private final FakePaymentGateway payments = new FakePaymentGateway();
    private final List<String> released = new ArrayList<>();
    private StartBookingPaymentService service;

    @BeforeEach
    void setUp() {
        StockReserver stock = new StockReserver() {
            @Override
            public boolean tryReserve(Booking booking) {
                return true;
            }

            @Override
            public void release(Booking booking) {
                released.add(booking.id());
            }
        };
        service = new StartBookingPaymentService(payments, repository, stock);
    }

    private Booking reserved(String id) {
        Booking booking = Booking.request(id, "user-1", "evt-1", Instant.parse("2026-01-01T10:00:00Z"),
                Duration.ofMinutes(10), 30, List.of(new BookingLine("t-1", "Early Bird", 2, 2500))).reserve();
        repository.save(booking);
        return booking;
    }

    @Test
    void the_booking_awaits_payment_with_the_stripe_client_secret() {
        service.execute(reserved("b-1"));

        Booking booking = repository.store.get("b-1");
        assertEquals(BookingStatus.AWAITING_PAYMENT, booking.status());
        assertEquals("pi_b-1", booking.payment().id());
        assertEquals("pi_b-1_secret_x", booking.payment().clientSecret());
        assertEquals(5060, booking.total());
        assertEquals(60, booking.fees());
        assertTrue(released.isEmpty());
        assertTrue(payments.cancelled.isEmpty());
    }

    @Test
    void the_payment_amount_sent_to_the_provider_is_the_total_with_fees() {
        service.execute(reserved("b-1"));

        assertEquals(1, payments.created.size());
        assertEquals(2 * 2500 + 2 * 30, payments.created.get(0).total());
    }

    @Test
    void a_provider_error_rejects_the_booking_and_gives_its_places_back() {
        payments.failOnCreate = true;

        assertDoesNotThrow(() -> service.execute(reserved("b-1")));

        Booking booking = repository.store.get("b-1");
        assertEquals(BookingStatus.REJECTED, booking.status());
        assertNull(booking.payment());
        assertEquals(List.of("b-1"), released);
    }

    @Test
    void places_are_not_released_twice_if_the_booking_expired_while_the_provider_failed() {
        Booking booking = reserved("b-1");
        payments.failOnCreate = true;
        // L'expiration (US-14) est passée entre-temps et a déjà rendu les places.
        repository.markExpired("b-1");

        service.execute(booking);

        assertEquals(BookingStatus.EXPIRED, repository.store.get("b-1").status());
        assertTrue(released.isEmpty());
    }

    @Test
    void a_booking_that_expired_during_the_provider_call_gets_its_payment_cancelled() {
        Booking booking = reserved("b-1");
        payments.duringCreate = () -> repository.markExpired("b-1");

        service.execute(booking);

        assertEquals(BookingStatus.EXPIRED, repository.store.get("b-1").status());
        assertNull(repository.store.get("b-1").payment());
        assertEquals(List.of("pi_b-1"), payments.cancelled);
        assertTrue(released.isEmpty()); // c'est l'expiration qui les rend, pas nous
    }

    @Test
    void a_failed_cancellation_is_only_logged() {
        Booking booking = reserved("b-1");
        payments.duringCreate = () -> repository.markExpired("b-1");
        payments.failOnCancel = true;

        assertDoesNotThrow(() -> service.execute(booking));

        assertEquals(BookingStatus.EXPIRED, repository.store.get("b-1").status());
    }
}
