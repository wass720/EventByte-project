package fr.soft_labs.eventbyte.booking.domain;

import java.time.Instant;
import java.util.List;

/**
 * Une réservation de tickets pour un seul événement.
 *
 * Immuable : chaque transition renvoie une nouvelle réservation. Une réservation naît
 * "requested", puis devient "reserved" (stock suffisant) ou "rejected" (stock insuffisant).
 * Les statuts suivants (paiement, expiration) arrivent avec US-14 à US-17.
 *
 * Montants en centimes. Domaine pur : aucune dépendance à Spring ni à JPA.
 */
public record Booking(String id,
                      String userId,
                      String eventId,
                      Instant bookedAt,
                      List<BookingLine> lines,
                      BookingStatus status,
                      int total,
                      int fees) {

    public Booking {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Une réservation contient au moins une ligne");
        }
        lines = List.copyOf(lines);
    }

    /** Nouvelle demande de réservation. Les frais sont à 0 jusqu'à l'Epic 5. */
    public static Booking request(String id, String userId, String eventId,
                                  Instant bookedAt, List<BookingLine> lines) {
        int total = lines.stream().mapToInt(BookingLine::subtotal).reduce(0, Math::addExact);
        return new Booking(id, userId, eventId, bookedAt, lines, BookingStatus.REQUESTED, total, 0);
    }

    /** Le stock a pu être bloqué. */
    public Booking reserve() {
        return transitionFromRequested(BookingStatus.RESERVED);
    }

    /** Le stock était insuffisant. */
    public Booking reject() {
        return transitionFromRequested(BookingStatus.REJECTED);
    }

    private Booking transitionFromRequested(BookingStatus target) {
        if (status != BookingStatus.REQUESTED) {
            throw new IllegalStateException(
                    "Transition impossible de " + status.value() + " vers " + target.value());
        }
        return new Booking(id, userId, eventId, bookedAt, lines, target, total, fees);
    }
}
