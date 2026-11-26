package fr.soft_labs.eventbyte.event.domain;

import java.time.Instant;
import java.util.List;

/**
 * Un événement publié par un organisateur dans Strapi, avec son lieu et ses tickets.
 *
 * Domaine pur : aucune dépendance à Spring, JPA ou au format des messages RabbitMQ.
 *
 * @param venue   peut être null si Strapi n'a pas envoyé de lieu
 * @param startAt peut être null si Strapi n'a pas envoyé de date
 */
public record Event(String id, String title, String description, String cover,
                    Venue venue, Instant startAt, List<EventTicket> tickets) {

    public Event {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Un événement doit avoir un id");
        }
        tickets = tickets == null ? List.of() : List.copyOf(tickets);
    }

    /** Prix du ticket le moins cher, en centimes. 0 s'il n'y a aucun ticket. */
    public int lowestPrice() {
        return tickets.stream().mapToInt(EventTicket::price).min().orElse(0);
    }

    /**
     * Complet quand plus aucun ticket n'est en stock (un événement sans ticket est
     * considéré complet : on ne peut rien y acheter).
     *
     * Ne tient compte que du stock reçu de Strapi, pas des réservations en cours.
     */
    public boolean isSoldOut() {
        return tickets.stream().allMatch(ticket -> ticket.quantity() == 0);
    }
}
