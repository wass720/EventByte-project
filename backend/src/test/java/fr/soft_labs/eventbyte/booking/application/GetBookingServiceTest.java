package fr.soft_labs.eventbyte.booking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetBookingServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private final InMemoryBookingRepository repository = new InMemoryBookingRepository();
    private GetBookingService getService;
    private ListUserBookingsService listService;

    @BeforeEach
    void setUp() {
        getService = new GetBookingService(repository);
        listService = new ListUserBookingsService(repository);
    }

    private void booking(String id, String userId, Instant bookedAt) {
        repository.save(Booking.request(id, userId, "evt", bookedAt, Duration.ofMinutes(10), 30,
                List.of(new BookingLine("t-1", "Early Bird", 1, 2500))).reserve());
    }

    @Test
    void the_owner_sees_their_booking() {
        booking("b-1", "user-1", T0);

        assertEquals("b-1", getService.execute("user-1", "b-1").orElseThrow().id());
    }

    @Test
    void an_unknown_booking_is_not_found() {
        assertTrue(getService.execute("user-1", "nope").isEmpty());
    }

    @Test
    void someone_elses_booking_looks_exactly_like_an_unknown_one() {
        booking("b-1", "user-1", T0);

        assertTrue(getService.execute("user-2", "b-1").isEmpty());
    }

    @Test
    void the_list_only_contains_my_bookings_most_recent_first() {
        booking("old", "user-1", T0);
        booking("new", "user-1", T0.plusSeconds(60));
        booking("other", "user-2", T0.plusSeconds(30));

        assertEquals(List.of("new", "old"),
                listService.execute("user-1").stream().map(Booking::id).toList());
    }

    @Test
    void the_list_is_empty_when_i_have_no_booking() {
        booking("b-1", "user-1", T0);

        assertTrue(listService.execute("user-2").isEmpty());
    }
}
