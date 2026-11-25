package fr.soft_labs.eventbyte.ticket.application.port;

import fr.soft_labs.eventbyte.ticket.domain.TicketStock;
import java.util.List;

/**
 * Port sortant : lecture des événements et de leurs tickets.
 */
public interface TicketCatalog {

    boolean eventExists(String eventId);

    List<TicketStock> findByEventId(String eventId);
}
