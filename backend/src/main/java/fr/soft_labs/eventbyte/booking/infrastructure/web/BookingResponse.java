package fr.soft_labs.eventbyte.booking.infrastructure.web;

import fr.soft_labs.eventbyte.booking.domain.Booking;
import java.util.List;

/**
 * Réservation au format du contrat d'API (README) : montants en centimes, date ISO 8601,
 * statut en minuscules. DTO distinct du domaine : userId, expiresAt n'en font pas partie.
 * clientSecret (Stripe) arrive avec US-16 ; le front refuse null, on l'omettra alors si absent.
 */
public record BookingResponse(String id,
                              String eventId,
                              String bookedAt,
                              List<Line> lines,
                              String status,
                              int total,
                              int fees) {

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
                booking.fees());
    }
}
