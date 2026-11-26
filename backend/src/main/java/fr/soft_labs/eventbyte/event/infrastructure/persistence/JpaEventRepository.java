package fr.soft_labs.eventbyte.event.infrastructure.persistence;

import fr.soft_labs.eventbyte.event.application.port.EventRepository;
import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.EventLocation;
import fr.soft_labs.eventbyte.event.domain.EventTicket;
import fr.soft_labs.eventbyte.event.domain.Venue;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Adapter Postgres (JPA) du port EventRepository : traduit le domaine en entités et inversement.
 * Les entités JPA restent dans l'infrastructure ; les use cases ne les voient jamais.
 */
@Repository
public class JpaEventRepository implements EventRepository {

    private final EventJpaRepository eventJpaRepository;
    private final TicketJpaRepository ticketJpaRepository;

    public JpaEventRepository(EventJpaRepository eventJpaRepository,
                              TicketJpaRepository ticketJpaRepository) {
        this.eventJpaRepository = eventJpaRepository;
        this.ticketJpaRepository = ticketJpaRepository;
    }

    /**
     * Idempotent : save() met à jour la ligne si l'id existe déjà, et les tickets sont
     * remplacés en bloc (suppression puis réinsertion) pour refléter exactement l'état
     * envoyé par Strapi. À appeler dans une transaction (voir EventIngestionService).
     */
    @Override
    public void save(Event event) {
        eventJpaRepository.save(toEntity(event));

        ticketJpaRepository.deleteByEventId(event.id());
        ticketJpaRepository.flush();
        ticketJpaRepository.saveAll(event.tickets().stream()
                .map(ticket -> new TicketEntity(ticket.id(), event.id(), ticket.name(),
                        ticket.quantity(), ticket.price()))
                .toList());
    }

    @Override
    public void deleteById(String eventId) {
        // Les tickets sont supprimés par la base (ON DELETE CASCADE, voir V2).
        eventJpaRepository.deleteById(eventId);
    }

    @Override
    public Optional<Event> findById(String eventId) {
        return eventJpaRepository.findById(eventId)
                .map(entity -> toDomain(entity, ticketJpaRepository.findByEventId(eventId)));
    }

    @Override
    public List<EventLocation> findAllLocations() {
        return eventJpaRepository.findAll().stream()
                .filter(entity -> entity.getLatitude() != null && entity.getLongitude() != null)
                .map(entity -> new EventLocation(entity.getId(), entity.getLatitude(), entity.getLongitude()))
                .toList();
    }

    private static EventEntity toEntity(Event event) {
        Venue venue = event.venue();
        return new EventEntity(
                event.id(),
                event.title(),
                event.description(),
                event.cover(),
                venue == null ? null : venue.name(),
                venue == null ? null : venue.address(),
                venue == null ? null : venue.latitude(),
                venue == null ? null : venue.longitude(),
                event.startAt());
    }

    private static Event toDomain(EventEntity entity, List<TicketEntity> tickets) {
        return new Event(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCover(),
                new Venue(entity.getVenueName(), entity.getVenueAddress(),
                        entity.getLatitude(), entity.getLongitude()),
                entity.getStartAt(),
                tickets.stream()
                        .map(ticket -> new EventTicket(ticket.getId(), ticket.getName(),
                                ticket.getPrice(), ticket.getQuantity()))
                        .toList());
    }
}
