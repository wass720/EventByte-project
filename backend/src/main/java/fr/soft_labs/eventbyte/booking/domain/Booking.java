package fr.soft_labs.eventbyte.booking.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Une réservation de tickets pour un seul événement.
 *
 * Immuable : chaque transition renvoie une nouvelle réservation. Une réservation naît
 * "requested", puis devient "reserved" (stock suffisant) ou "rejected" (stock insuffisant).
 * Tant qu'elle n'est pas payée, elle expire à "expiresAt" (US-14) et passe "expired".
 * Les statuts de paiement (awaiting-payment, confirmed) arrivent avec US-16 et US-17.
 *
 * Montants en centimes. Domaine pur : aucune dépendance à Spring ni à JPA.
 */
public record Booking(String id,
                      String userId,
                      String eventId,
                      Instant bookedAt,
                      Instant expiresAt,
                      List<BookingLine> lines,
                      BookingStatus status,
                      int total,
                      int fees) {

    /** Statuts où des places sont bloquées sans avoir été payées : ils peuvent expirer. */
    public static final Set<BookingStatus> UNPAID_STATUSES =
            Set.of(BookingStatus.RESERVED, BookingStatus.AWAITING_PAYMENT);

    public Booking {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Une réservation contient au moins une ligne");
        }
        lines = List.copyOf(lines);
    }

    /**
     * Nouvelle demande de réservation. Les frais sont à 0 jusqu'à l'Epic 5.
     *
     * @param paymentWindow temps laissé pour payer, à partir de bookedAt
     */
    public static Booking request(String id, String userId, String eventId, Instant bookedAt,
                                  Duration paymentWindow, List<BookingLine> lines) {
        int total = lines.stream().mapToInt(BookingLine::subtotal).reduce(0, Math::addExact);
        return new Booking(id, userId, eventId, bookedAt, bookedAt.plus(paymentWindow),
                lines, BookingStatus.REQUESTED, total, 0);
    }

    /** Le stock a pu être bloqué. */
    public Booking reserve() {
        return transition(Set.of(BookingStatus.REQUESTED), BookingStatus.RESERVED);
    }

    /** Le stock était insuffisant. */
    public Booking reject() {
        return transition(Set.of(BookingStatus.REQUESTED), BookingStatus.REJECTED);
    }

    /** Le délai de paiement est dépassé : les places repartent en vente. */
    public Booking expire() {
        return transition(UNPAID_STATUSES, BookingStatus.EXPIRED);
    }

    /** Vrai si la réservation n'est pas payée et que son délai est atteint. */
    public boolean isDueForExpiration(Instant now) {
        return UNPAID_STATUSES.contains(status) && !now.isBefore(expiresAt);
    }

    private Booking transition(Set<BookingStatus> allowedFrom, BookingStatus target) {
        if (!allowedFrom.contains(status)) {
            throw new IllegalStateException(
                    "Transition impossible de " + status.value() + " vers " + target.value());
        }
        return new Booking(id, userId, eventId, bookedAt, expiresAt, lines, target, total, fees);
    }
}
