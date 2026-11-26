package fr.soft_labs.eventbyte.booking.application;

import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Faux dépôt en mémoire pour les tests de use cases. */
class InMemoryBookingRepository implements BookingRepository {

    final Map<String, Booking> store = new LinkedHashMap<>();

    @Override
    public void save(Booking booking) {
        store.put(booking.id(), booking);
    }

    @Override
    public boolean update(Booking booking, BookingStatus expectedStatus) {
        Booking current = store.get(booking.id());
        if (current == null || current.status() != expectedStatus) {
            return false;
        }
        store.put(booking.id(), booking);
        return true;
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        return Optional.ofNullable(store.get(bookingId));
    }

    @Override
    public Optional<Booking> findByPaymentId(String paymentId) {
        return store.values().stream()
                .filter(b -> b.payment() != null && b.payment().id().equals(paymentId))
                .findFirst();
    }

    @Override
    public List<Booking> findByUserId(String userId) {
        return store.values().stream()
                .filter(b -> b.userId().equals(userId))
                .sorted(Comparator.comparing(Booking::bookedAt).reversed())
                .toList();
    }

    @Override
    public List<Booking> findDueForExpiration(Instant now, int limit) {
        return store.values().stream().filter(b -> b.isDueForExpiration(now)).limit(limit).toList();
    }

    @Override
    public boolean markExpired(String bookingId) {
        Booking booking = store.get(bookingId);
        if (booking == null || !Booking.UNPAID_STATUSES.contains(booking.status())) {
            return false;
        }
        store.put(bookingId, booking.expire());
        return true;
    }
}
