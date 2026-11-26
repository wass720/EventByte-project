package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.application.port.EventSearchIndex;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fake en mémoire de l'index Meilisearch : garde le dernier document de chaque événement. */
class InMemorySearchIndex implements EventSearchIndex {

    final Map<String, SearchableEvent> documents = new LinkedHashMap<>();
    final List<String> searchResult = new ArrayList<>();

    @Override
    public void index(SearchableEvent event) {
        documents.put(event.id(), event);
    }

    @Override
    public void remove(String eventId) {
        documents.remove(eventId);
    }

    @Override
    public List<String> search(String query, double lat, double lng, double radiusKm) {
        return List.copyOf(searchResult);
    }
}
