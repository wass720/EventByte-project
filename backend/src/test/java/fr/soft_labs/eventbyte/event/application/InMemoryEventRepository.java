package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.application.port.EventRepository;
import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.EventLocation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fake en mémoire du port EventRepository : même contrat (upsert, suppression idempotente). */
class InMemoryEventRepository implements EventRepository {

    private final Map<String, Event> events = new LinkedHashMap<>();

    @Override
    public void save(Event event) {
        events.put(event.id(), event);
    }

    @Override
    public void deleteById(String eventId) {
        events.remove(eventId);
    }

    @Override
    public Optional<Event> findById(String eventId) {
        return Optional.ofNullable(events.get(eventId));
    }

    @Override
    public List<EventLocation> findAllLocations() {
        return events.values().stream()
                .filter(event -> event.venue() != null && event.venue().hasLocation())
                .map(event -> new EventLocation(event.id(), event.venue().latitude(), event.venue().longitude()))
                .toList();
    }

    int count() {
        return events.size();
    }
}
