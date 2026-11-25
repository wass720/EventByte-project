package fr.soft_labs.eventbyte.ticket.infrastructure.persistence;

import fr.soft_labs.eventbyte.event.infrastructure.persistence.EventJpaRepository;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketEntity;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketJpaRepository;
import fr.soft_labs.eventbyte.ticket.application.port.TicketCatalog;
import fr.soft_labs.eventbyte.ticket.domain.TicketStock;
import java.util.List;
import org.springframework.stereotype.Component;

/** Adapter du port TicketCatalog sur les tables events / tickets (alimentées par Strapi). */
@Component
public class JpaTicketCatalog implements TicketCatalog {

    private final EventJpaRepository eventRepository;
    private final TicketJpaRepository ticketRepository;

    public JpaTicketCatalog(EventJpaRepository eventRepository, TicketJpaRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public boolean eventExists(String eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public List<TicketStock> findByEventId(String eventId) {
        return ticketRepository.findByEventId(eventId).stream()
                .map(JpaTicketCatalog::toDomain)
                .toList();
    }

    private static TicketStock toDomain(TicketEntity entity) {
        return new TicketStock(entity.getId(), entity.getEventId(), entity.getName(),
                entity.getPrice(), entity.getQuantity());
    }
}
