package fr.soft_labs.eventbyte.booking.infrastructure.persistence;

import fr.soft_labs.eventbyte.booking.application.port.BookableTickets;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketJpaRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

/** Adapter du port BookableTickets sur la table tickets (alimentée par Strapi). */
@Component
public class JpaBookableTickets implements BookableTickets {

    private final TicketJpaRepository ticketRepository;

    public JpaBookableTickets(TicketJpaRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public List<BookableTicket> findAllById(Collection<String> ticketIds) {
        return ticketRepository.findAllById(ticketIds).stream()
                .map(t -> new BookableTicket(t.getId(), t.getEventId(), t.getName(), t.getPrice()))
                .toList();
    }
}
