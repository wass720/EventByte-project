package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.infrastructure.persistence.EventEntity;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.EventJpaRepository;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketEntity;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketJpaRepository;
import fr.soft_labs.eventbyte.event.infrastructure.web.EventResponse;
import fr.soft_labs.eventbyte.like.application.port.LikeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GetEventByIdService {

    private final EventJpaRepository eventRepository;
    private final TicketJpaRepository ticketRepository;
    private final LikeRepository likeRepository;

    public GetEventByIdService(EventJpaRepository eventRepository,
                               TicketJpaRepository ticketRepository,
                               LikeRepository likeRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.likeRepository = likeRepository;
    }

    public Optional<EventResponse> execute(String id) {
        return eventRepository.findById(id)
                .map(event -> toResponse(event, ticketRepository.findByEventId(id)));
    }

    private EventResponse toResponse(EventEntity event, List<TicketEntity> tickets) {
        int lowestPrice = tickets.stream()
                .mapToInt(TicketEntity::getPrice)
                .min()
                .orElse(0);

        boolean isSoldOut = tickets.stream()
                .allMatch(ticket -> ticket.getQuantity() == 0);

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getCover(),
                likeRepository.countByEventId(event.getId()),
                new EventResponse.Venue(
                        event.getVenueName(),
                        event.getVenueAddress(),
                        new EventResponse.Coordinates(event.getLatitude(), event.getLongitude())),
                isSoldOut,
                null,              // startAt : pas encore envoyé par Strapi
                lowestPrice,
                event.getDescription());
    }
}