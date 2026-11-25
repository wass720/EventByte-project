package fr.soft_labs.eventbyte.booking.application.port;

import java.util.Collection;
import java.util.List;

/** Port sortant : les types de tickets que l'on peut réserver (alimentés par Strapi). */
public interface BookableTickets {

    /** Ne renvoie que les tickets qui existent : un id inconnu est simplement absent. */
    List<BookableTicket> findAllById(Collection<String> ticketIds);

    record BookableTicket(String id, String eventId, String name, int price) {
    }
}
