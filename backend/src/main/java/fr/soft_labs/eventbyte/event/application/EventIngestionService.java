package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.application.port.EventSearchIndex;
import fr.soft_labs.eventbyte.event.application.port.EventSearchIndex.SearchableEvent;
import fr.soft_labs.eventbyte.event.infrastructure.messaging.EventMessage;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.EventEntity;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.EventJpaRepository;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketEntity;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketJpaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventIngestionService {

    private final EventJpaRepository eventRepository;
    private final TicketJpaRepository ticketRepository;
    private final EventSearchIndex searchIndex;


    public EventIngestionService(EventJpaRepository eventRepository,
                                 TicketJpaRepository ticketRepository,
                                 EventSearchIndex searchIndex) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.searchIndex = searchIndex;
    }

    @Transactional
    public void upsert(EventMessage message) {
        EventMessage.Venue venue = message.venue();
        EventEntity event = new EventEntity(
                message.id(),
                message.title(),
                message.description(),
                message.cover(),
                venue == null ? null : venue.name(),
                venue == null ? null : venue.address(),
                venue == null ? null : venue.latitude(),
                venue == null ? null : venue.longitude(),
                message.startAt() == null ? null : Instant.parse(message.startAt()));
        eventRepository.save(event);

        ticketRepository.deleteByEventId(message.id());
        ticketRepository.flush();
        if (message.tickets() != null) {
            List<TicketEntity> tickets = message.tickets().stream()
                    .map(t -> new TicketEntity(t.id(), message.id(), t.name(), t.quantity(), t.price()))
                    .toList();
            ticketRepository.saveAll(tickets);
        }

        searchIndex.index(new SearchableEvent(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLatitude(),
                event.getLongitude()));
    }

    @Transactional
    public void delete(String eventId) {
        eventRepository.deleteById(eventId);
        searchIndex.remove(eventId);
    }
}