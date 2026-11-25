package fr.soft_labs.eventbyte.booking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
            new BookableTicket("t-3", "evt-2", "Standard", 1000));

    private final InMemoryBookings bookings = new InMemoryBookings();
    private boolean stockAvailable;
    private final List<Booking> reserveAttempts = new ArrayList<>();
    private final List<Booking> releases = new ArrayList<>();
    private boolean failOnDecidedSave;
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
                Clock.fixed(NOW, ZoneOffset.UTC), new BookingProperties(Duration.ofMinutes(10)));
    }

    @Test
    void reserves_the_booking_when_the_stock_suffices() {
        String id = service.execute("user-1", List.of(new Item("t-1", 2), new Item("t-2", 1)));

        Booking booking = bookings.get(id);
        assertEquals(BookingStatus.RESERVED, booking.status());
        assertEquals("user-1", booking.userId());
        assertEquals("evt-1", booking.eventId());
        assertEquals(NOW, booking.bookedAt());
        assertEquals(NOW.plus(Duration.ofMinutes(10)), booking.expiresAt());
        assertEquals(14000, booking.total());
        assertEquals(2, booking.lines().size());
    }

    @Test
    void rejects_the_booking_when_the_stock_is_insufficient() {
        stockAvailable = false;

        String id = service.execute("user-1", List.of(new Item("t-1", 2)));

        assertEquals(BookingStatus.REJECTED, bookings.get(id).status());
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
        assertEquals(12500, booking.total());
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
