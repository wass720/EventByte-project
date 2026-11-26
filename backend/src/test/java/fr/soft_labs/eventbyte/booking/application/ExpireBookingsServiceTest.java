package fr.soft_labs.eventbyte.booking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.application.port.StockReserver;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpireBookingsServiceTest {

    private static final Instant BOOKED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final Map<String, Booking> store = new LinkedHashMap<>();
    private final List<String> released = new ArrayList<>();
    private Instant now;
    private final FakePaymentGateway payments = new FakePaymentGateway();
    private final RecordingBookingNotifier notifier = new RecordingBookingNotifier();
    private ExpireBookingsService service;

    @BeforeEach
    void setUp() {
        now = BOOKED_AT.plus(WINDOW).plusSeconds(1);
        BookingRepository repository = new BookingRepository() {
            @Override
            public void save(Booking booking) {
                store.put(booking.id(), booking);
            }

            @Override
            public boolean update(Booking booking, BookingStatus expectedStatus) {
                throw new UnsupportedOperationException("pas utilisé par l'expiration");
            }

            @Override
            public java.util.Optional<Booking> findByPaymentId(String paymentId) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<Booking> findById(String bookingId) {
                return java.util.Optional.ofNullable(store.get(bookingId));
            }

            @Override
            public List<Booking> findByUserId(String userId) {
                return List.of();
            }

            @Override
            public List<Booking> findDueForExpiration(Instant at, int limit) {
                return store.values().stream().filter(b -> b.isDueForExpiration(at)).limit(limit).toList();
            }

            @Override
            public boolean markExpired(String bookingId) {
                Booking booking = store.get(bookingId);
                if (!Booking.UNPAID_STATUSES.contains(booking.status())) {
                    return false;
                }
                store.put(bookingId, booking.expire());
                return true;
            }
        };
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
        service = new ExpireBookingsService(repository, stock, payments, notifier,
                new Clock() {
                    @Override
                    public ZoneOffset getZone() {
                        return ZoneOffset.UTC;
                    }

                    @Override
                    public Clock withZone(java.time.ZoneId zone) {
                        return this;
                    }

                    @Override
                    public Instant instant() {
                        return now;
                    }
                });
    }

    private Booking reserved(String id) {
        Booking booking = Booking.request(id, "user", "evt", BOOKED_AT, WINDOW, 30,
                List.of(new BookingLine("t-1", "Early Bird", 2, 2500))).reserve();
        store.put(id, booking);
        return booking;
    }

    private Booking awaitingPayment(String id) {
        Booking booking = reserved(id).awaitPayment(FakePaymentGateway.paymentOf(id));
        store.put(id, booking);
        return booking;
    }

    @Test
    void an_expired_booking_awaiting_payment_has_its_stripe_payment_cancelled() {
        awaitingPayment("b-1");

        assertEquals(1, service.execute());

        assertEquals(BookingStatus.EXPIRED, store.get("b-1").status());
        assertEquals(List.of("pi_b-1"), payments.cancelled);
        assertEquals(List.of("b-1"), released);
        assertEquals(List.of(BookingStatus.EXPIRED), notifier.statuses());
        assertEquals("b-1", notifier.notified.get(0).id());
    }

    @Test
    void nobody_is_notified_when_nothing_expires() {
        awaitingPayment("b-1");
        payments.failOnCancel = true;

        service.execute();

        assertTrue(notifier.notified.isEmpty());
    }

    @Test
    void a_booking_whose_payment_cannot_be_cancelled_keeps_its_places_until_a_later_run() {
        // Paiement déjà effectué (pas encore confirmé, US-17) ou Stripe indisponible :
        // rendre les places permettrait de les vendre deux fois.
        awaitingPayment("b-1");
        payments.failOnCancel = true;

        assertEquals(0, service.execute());

        assertEquals(BookingStatus.AWAITING_PAYMENT, store.get("b-1").status());
        assertTrue(released.isEmpty());

        payments.failOnCancel = false;
        assertEquals(1, service.execute());
        assertEquals(BookingStatus.EXPIRED, store.get("b-1").status());
        assertEquals(List.of("b-1"), released);
    }

    @Test
    void a_full_batch_of_uncancellable_payments_does_not_loop_forever() {
        for (int i = 0; i < 150; i++) {
            awaitingPayment("b-" + i);
        }
        payments.failOnCancel = true;

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertEquals(0, service.execute()));
        assertTrue(released.isEmpty());
    }

    @Test
    void the_payment_is_cancelled_before_the_booking_expires() {
        awaitingPayment("b-1");
        List<BookingStatus> statusWhenCancelled = new ArrayList<>();
        payments.duringCancel = () -> statusWhenCancelled.add(store.get("b-1").status());

        service.execute();

        assertEquals(List.of(BookingStatus.AWAITING_PAYMENT), statusWhenCancelled);
    }

    @Test
    void a_reserved_booking_without_payment_expires_without_calling_the_provider() {
        reserved("b-1");

        service.execute();

        assertTrue(payments.cancelled.isEmpty());
        assertEquals(List.of("b-1"), released);
    }

    @Test
    void a_booking_awaiting_payment_inside_its_window_keeps_its_payment() {
        now = BOOKED_AT.plus(WINDOW).minusSeconds(1);
        awaitingPayment("b-1");

        assertEquals(0, service.execute());

        assertEquals(BookingStatus.AWAITING_PAYMENT, store.get("b-1").status());
        assertTrue(payments.cancelled.isEmpty());
    }

    @Test
    void an_unpaid_booking_past_its_deadline_expires_and_gives_its_places_back() {
        reserved("b-1");

        assertEquals(1, service.execute());

        assertEquals(BookingStatus.EXPIRED, store.get("b-1").status());
        assertEquals(List.of("b-1"), released);
    }

    @Test
    void a_booking_still_inside_its_window_is_left_alone() {
        now = BOOKED_AT.plus(WINDOW).minusSeconds(1);
        reserved("b-1");

        assertEquals(0, service.execute());

        assertEquals(BookingStatus.RESERVED, store.get("b-1").status());
        assertTrue(released.isEmpty());
    }

    @Test
    void places_are_given_back_only_once_even_if_it_runs_again() {
        reserved("b-1");

        service.execute();
        assertEquals(0, service.execute());

        assertEquals(List.of("b-1"), released);
    }

    @Test
    void a_booking_paid_in_the_meantime_is_not_expired_and_keeps_its_places() {
        Booking booking = reserved("b-1");
        // Le paiement passe entre la lecture et le markExpired : le statut n'est plus "non payé".
        store.put("b-1", new Booking(booking.id(), booking.userId(), booking.eventId(),
                booking.bookedAt(), booking.expiresAt(), booking.lines(),
                BookingStatus.CONFIRMED, booking.total(), booking.fees(), booking.payment()));

        assertEquals(0, service.execute());

        assertEquals(BookingStatus.CONFIRMED, store.get("b-1").status());
        assertTrue(released.isEmpty());
    }

    @Test
    void rejected_and_requested_bookings_never_expire() {
        store.put("r", Booking.request("r", "u", "evt", BOOKED_AT, WINDOW, 30,
                List.of(new BookingLine("t-1", "A", 1, 100))).reject());
        store.put("q", Booking.request("q", "u", "evt", BOOKED_AT, WINDOW, 30,
                List.of(new BookingLine("t-1", "A", 1, 100))));

        assertEquals(0, service.execute());
        assertTrue(released.isEmpty());
    }

    @Test
    void every_due_booking_is_handled_across_several_batches() {
        for (int i = 0; i < 250; i++) {
            reserved("b-" + i);
        }

        assertEquals(250, service.execute());

        assertEquals(250, released.size());
        assertTrue(store.values().stream().allMatch(b -> b.status() == BookingStatus.EXPIRED));
    }
}
