package fr.soft_labs.eventbyte.booking.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.util.List;

/**
 * Réservation au format du contrat d'API (README) : montants en centimes (total frais inclus),
 * date ISO 8601, statut en minuscules. DTO distinct du domaine : userId, expiresAt n'en font
 * pas partie.
 *
 * clientSecret (Stripe, US-16) n'est présent que tant que la réservation attend son paiement :
 * c'est le seul moment où le frontend en a besoin. Absent sinon (le frontend refuse null).
 */
public record BookingResponse(String id,
                              String eventId,
                              String bookedAt,
                              List<Line> lines,
                              String status,
                              int total,
                              int fees,
                              @JsonInclude(JsonInclude.Include.NON_NULL) String clientSecret) {

    public record Line(String id, String name, int quantity, int price) {
    }

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.id(),
                booking.eventId(),
                booking.bookedAt().toString(),
                booking.lines().stream()
                        .map(l -> new Line(l.ticketId(), l.name(), l.quantity(), l.unitPrice()))
                        .toList(),
                booking.status().value(),
                booking.total(),
                booking.fees(),
                booking.status() == BookingStatus.AWAITING_PAYMENT && booking.payment() != null
                        ? booking.payment().clientSecret()
                        : null);
    }
}
