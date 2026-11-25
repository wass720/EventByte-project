package fr.soft_labs.eventbyte.like.infrastructure.persistence;

import fr.soft_labs.eventbyte.event.infrastructure.persistence.EventJpaRepository;
import fr.soft_labs.eventbyte.like.application.port.EventCatalog;
import org.springframework.stereotype.Component;

@Component
public class JpaEventCatalog implements EventCatalog {

    private final EventJpaRepository eventRepository;

    public JpaEventCatalog(EventJpaRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public boolean exists(String eventId) {
        return eventRepository.existsById(eventId);
    }
}
