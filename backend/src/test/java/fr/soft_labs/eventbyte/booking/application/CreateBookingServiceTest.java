package fr.soft_labs.eventbyte.booking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.soft_labs.eventbyte.booking.application.CreateBookingService.Item;
import fr.soft_labs.eventbyte.booking.application.port.BookableTickets;
import fr.soft_labs.eventbyte.booking.application.port.BookableTickets.BookableTicket;
import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.application.port.StockReserver;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateBookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final List<BookableTicket> catalog = List.of(
            new BookableTicket("t-1", "evt-1", "Early Bird", 2500),
            new BookableTicket("t-2", "evt-1", "VIP", 9000),
            new BookableTicket("t-3", "evt-2", "Standard", 1000),
            new BookableTicket("free", "evt-1", "Gratuit", 0));

    private final InMemoryBookings bookings = new InMemoryBookings();
    private boolean stockAvailable;
    private final List<Booking> reserveAttempts = new ArrayList<>();
    private final List<Booking> releases = new ArrayList<>();
    private boolean failOnDecidedSave;
    private final FakePaymentGateway payments = new FakePaymentGateway();
    private final RecordingBookingNotifier notifier = new RecordingBookingNotifier();
    private CreateBookingService service;

    @BeforeEach
    void setUp() {
        stockAvailable = true;
        failOnDecidedSave = false;
        BookableTickets tickets = ids -> catalog.stream().filter(t -> ids.contains(t.id())).toList();
        StockReserver stock = new StockReserver() {
            @Override
            public boolean tryReserve(Booking booking) {
                reserveAttempts.add(booking);
                return stockAvailable;
            }

            @Override
            public void release(Booking booking) {
                releases.add(booking);
            }
        };
        service = new CreateBookingService(tickets, stock, bookings,
                new StartBookingPaymentService(payments, bookings, stock, notifier), notifier,
                Clock.fixed(NOW, ZoneOffset.UTC), new BookingProperties(Duration.ofMinutes(10), 30, 50));
    }

    @Test
    void a_booking_with_enough_stock_ends_awaiting_its_card_payment() {
        String id = service.execute("user-1", List.of(new Item("t-1", 2), new Item("t-2", 1)));

        Booking booking = bookings.get(id);
        assertEquals(BookingStatus.AWAITING_PAYMENT, booking.status());
        assertEquals(FakePaymentGateway.paymentOf(id), booking.payment());
        assertEquals("user-1", booking.userId());
        assertEquals("evt-1", booking.eventId());
        assertEquals(NOW, booking.bookedAt());
        assertEquals(NOW.plus(Duration.ofMinutes(10)), booking.expiresAt());
        assertEquals(2, booking.lines().size());
    }

    @Test
    void amounts_are_in_cents_with_fees_per_ticket_included_in_the_total() {
        String id = service.execute("user-1", List.of(new Item("t-1", 2), new Item("t-2", 1)));

        Booking booking = bookings.get(id);
        assertEquals(90, booking.fees());                  // 3 tickets x 30 centimes
        assertEquals(2 * 2500 + 9000 + 90, booking.total());
        // Stripe reçoit exactement ce total.
        assertEquals(booking.total(), payments.created.get(0).total());
    }

    @Test
    void a_total_below_the_card_minimum_is_refused_before_touching_the_stock() {
        // 1 ticket gratuit = 30 centimes de frais < 50 : Stripe refuserait le paiement.
        assertThrows(InvalidBookingException.class,
                () -> service.execute("user-1", List.of(new Item("free", 1))));

        assertTrue(reserveAttempts.isEmpty());
        assertTrue(bookings.savedStatuses.isEmpty());
        assertTrue(payments.created.isEmpty());
    }

    @Test
    void a_total_reaching_the_card_minimum_is_accepted() {
        // 2 tickets gratuits = 60 centimes de frais >= 50.
        String id = service.execute("user-1", List.of(new Item("free", 2)));

        assertEquals(BookingStatus.AWAITING_PAYMENT, bookings.get(id).status());
        assertEquals(60, bookings.get(id).total());
    }

    @Test
    void the_owner_is_notified_of_each_status_change() {
        service.execute("user-1", List.of(new Item("t-1", 1)));
        stockAvailable = false;
        service.execute("user-1", List.of(new Item("t-1", 1)));

        assertEquals(List.of(BookingStatus.RESERVED, BookingStatus.AWAITING_PAYMENT, BookingStatus.REJECTED),
                notifier.statuses());
        assertTrue(notifier.notified.stream().allMatch(b -> b.userId().equals("user-1")));
    }

    @Test
    void the_payment_is_prepared_once_the_places_are_reserved() {
        service.execute("user-1", List.of(new Item("t-1", 1)));

        assertEquals(1, payments.created.size());
        assertEquals(BookingStatus.RESERVED, payments.created.get(0).status());
        assertEquals(List.of(BookingStatus.REQUESTED, BookingStatus.RESERVED), bookings.savedStatuses);
    }

    @Test
    void rejects_the_booking_when_the_stock_is_insufficient() {
        stockAvailable = false;

        String id = service.execute("user-1", List.of(new Item("t-1", 2)));

        assertEquals(BookingStatus.REJECTED, bookings.get(id).status());
        assertTrue(payments.created.isEmpty());
    }

    @Test
    void the_booking_is_rejected_and_its_places_released_if_the_payment_cannot_be_prepared() {
        payments.failOnCreate = true;

        String id = service.execute("user-1", List.of(new Item("t-1", 2)));

        Booking booking = bookings.get(id);
        assertEquals(BookingStatus.REJECTED, booking.status());
        assertNull(booking.payment());
        assertEquals(1, releases.size());
        assertEquals(id, releases.get(0).id());
    }

    @Test
    void the_booking_is_saved_as_requested_before_the_stock_decision() {
        service.execute("user-1", List.of(new Item("t-1", 1)));

        assertEquals(List.of(BookingStatus.REQUESTED, BookingStatus.RESERVED), bookings.savedStatuses);
        assertEquals(BookingStatus.REQUESTED, reserveAttempts.get(0).status());
    }

    @Test
    void merges_several_lines_for_the_same_ticket() {
        String id = service.execute("user-1", List.of(new Item("t-1", 2), new Item("t-1", 3)));

        Booking booking = bookings.get(id);
        assertEquals(1, booking.lines().size());
        assertEquals(5, booking.lines().get(0).quantity());
        assertEquals(12500 + 5 * 30, booking.total());
    }

    @Test
    void an_unknown_ticket_is_refused_without_touching_the_stock() {
        assertThrows(TicketNotFoundException.class,
                () -> service.execute("user-1", List.of(new Item("t-1", 1), new Item("nope", 1))));

        assertTrue(reserveAttempts.isEmpty());
        assertTrue(bookings.savedStatuses.isEmpty());
    }

    @Test
    void tickets_of_several_events_are_refused() {
        assertThrows(InvalidBookingException.class,
                () -> service.execute("user-1", List.of(new Item("t-1", 1), new Item("t-3", 1))));

        assertTrue(bookings.savedStatuses.isEmpty());
    }

    @Test
    void invalid_requests_are_refused() {
        assertThrows(InvalidBookingException.class, () -> service.execute("user-1", null));
        assertThrows(InvalidBookingException.class, () -> service.execute("user-1", List.of()));
        assertThrows(InvalidBookingException.class,
                () -> service.execute("user-1", List.of(new Item("t-1", 0))));
        assertThrows(InvalidBookingException.class,
                () -> service.execute("user-1", List.of(new Item("t-1", -2))));
        assertThrows(InvalidBookingException.class,
                () -> service.execute("user-1", List.of(new Item(" ", 1))));

        assertTrue(reserveAttempts.isEmpty());
    }

    @Test
    void the_blocked_places_are_released_if_the_final_status_cannot_be_saved() {
        failOnDecidedSave = true;

        assertThrows(IllegalStateException.class,
                () -> service.execute("user-1", List.of(new Item("t-1", 2))));

        assertEquals(1, releases.size());
        assertEquals(2, releases.get(0).lines().get(0).quantity());
        assertTrue(payments.created.isEmpty());
    }

    @Test
    void nothing_is_released_when_the_booking_is_rejected_or_saved() {
        service.execute("user-1", List.of(new Item("t-1", 1)));
        stockAvailable = false;
        service.execute("user-1", List.of(new Item("t-1", 1)));

        assertTrue(releases.isEmpty());
    }

    @Test
    void every_booking_gets_its_own_id() {
        String first = service.execute("user-1", List.of(new Item("t-1", 1)));
        String second = service.execute("user-1", List.of(new Item("t-1", 1)));

        assertNotNull(first);
        assertTrue(!first.equals(second));
    }

    private class InMemoryBookings implements BookingRepository {
        private final Map<String, Booking> byId = new LinkedHashMap<>();
        private final List<BookingStatus> savedStatuses = new ArrayList<>();

        @Override
        public void save(Booking booking) {
            if (failOnDecidedSave && booking.status() != BookingStatus.REQUESTED) {
                throw new IllegalStateException("base indisponible");
            }
            byId.put(booking.id(), booking);
            savedStatuses.add(booking.status());
        }

        @Override
        public boolean update(Booking booking, BookingStatus expectedStatus) {
            Booking current = byId.get(booking.id());
            if (current == null || current.status() != expectedStatus) {
                return false;
            }
            byId.put(booking.id(), booking);
            return true;
        }

        @Override
        public java.util.Optional<Booking> findByPaymentId(String paymentId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<Booking> findById(String bookingId) {
            return java.util.Optional.ofNullable(byId.get(bookingId));
        }

        @Override
        public List<Booking> findByUserId(String userId) {
            return List.of();
        }

        @Override
        public List<Booking> findDueForExpiration(Instant now, int limit) {
            return List.of();
        }

        @Override
        public boolean markExpired(String bookingId) {
            return false;
        }

        Booking get(String id) {
            return byId.get(id);
        }
    }
}
