package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.application.port.EventSearchIndex;
import java.util.List;
import org.springframework.stereotype.Service;

/** US-7 : recherche par mot-clé dans un rayon (Meilisearch), détail depuis la base. */
@Service
public class SearchEventsService {

    private final EventSearchIndex searchIndex;
    private final GetEventsByIdsService getEventsByIdsService;

    public SearchEventsService(EventSearchIndex searchIndex,
                               GetEventsByIdsService getEventsByIdsService) {
        this.searchIndex = searchIndex;
        this.getEventsByIdsService = getEventsByIdsService;
    }

    public List<EventDetails> execute(String query, double lat, double lng, double radiusKm) {
        List<String> ids = searchIndex.search(query, lat, lng, radiusKm);
        return getEventsByIdsService.execute(ids);
    }
}
