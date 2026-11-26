package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.application.port.EventRepository;
import fr.soft_labs.eventbyte.event.application.port.EventSearchIndex;
import fr.soft_labs.eventbyte.event.application.port.EventSearchIndex.SearchableEvent;
import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.Venue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-2 : répercute dans le backend les événements créés, modifiés ou supprimés dans Strapi.
 *
 * Un même message peut arriver plusieurs fois (RabbitMQ garantit "au moins une fois",
 * et l'outbox de Strapi renvoie les messages non acquittés). Chaque opération est donc
 * idempotente : rejouer un message ne crée aucun doublon et ne provoque aucune erreur.
 */
@Service
public class EventIngestionService {

    private final EventRepository eventRepository;
    private final EventSearchIndex searchIndex;

    public EventIngestionService(EventRepository eventRepository, EventSearchIndex searchIndex) {
        this.eventRepository = eventRepository;
        this.searchIndex = searchIndex;
    }

    /** event.created et event.updated : l'état reçu remplace l'état connu. */
    @Transactional
    public void upsert(Event event) {
        eventRepository.save(event);

        Venue venue = event.venue();
        searchIndex.index(new SearchableEvent(
                event.id(),
                event.title(),
                event.description(),
                venue == null ? null : venue.latitude(),
                venue == null ? null : venue.longitude()));
    }

    /** event.deleted : sans effet si l'événement est déjà supprimé ou inconnu. */
    @Transactional
    public void delete(String eventId) {
        eventRepository.deleteById(eventId);
        searchIndex.remove(eventId);
    }
}
