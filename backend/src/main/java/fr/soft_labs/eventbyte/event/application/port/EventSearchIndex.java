package fr.soft_labs.eventbyte.event.application.port;
import java.util.List;

public interface EventSearchIndex {

    void index(SearchableEvent event);

    void remove(String eventId);

    List<String> search(String query, double lat, double lng, double radiusKm);

    record SearchableEvent(String id, String title, String description,
                           Double latitude, Double longitude) {
    }
}