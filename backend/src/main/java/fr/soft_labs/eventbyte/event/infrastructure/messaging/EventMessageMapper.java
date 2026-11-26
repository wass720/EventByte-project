package fr.soft_labs.eventbyte.event.infrastructure.messaging;

import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.EventTicket;
import fr.soft_labs.eventbyte.event.domain.Venue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Traduit un message Strapi (format RabbitMQ) en objet du domaine.
 * Toutes les conversions propres au format de Strapi sont ici, pas dans le domaine.
 */
final class EventMessageMapper {

    private EventMessageMapper() {
    }

    static Event toDomain(EventMessage message) {
        EventMessage.Venue venue = message.venue();
        List<EventTicket> tickets = message.tickets() == null ? List.of() : message.tickets().stream()
                .map(EventMessageMapper::toDomain)
                .toList();

        return new Event(
                message.id(),
                message.title(),
                message.description(),
                message.cover(),
                venue == null ? null : new Venue(venue.name(), venue.address(), venue.latitude(), venue.longitude()),
                message.startAt() == null ? null : Instant.parse(message.startAt()),
                tickets);
    }

    private static EventTicket toDomain(EventMessage.Ticket ticket) {
        Integer price = toCents(ticket.price());
        if (price == null || ticket.quantity() == null) {
            throw new IllegalArgumentException("Ticket sans prix ou sans quantité : " + ticket.id());
        }
        return new EventTicket(ticket.id(), ticket.name(), price, ticket.quantity());
    }

    /**
     * Strapi saisit les prix en euros (décimal) ; tout le backend et l'API travaillent en
     * centimes (entiers), l'unité attendue par Stripe. 25.5 € -> 2550. Arrondi au centime.
     */
    static Integer toCents(BigDecimal euros) {
        if (euros == null) {
            return null;
        }
        return euros.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}
