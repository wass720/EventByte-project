package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.application.port.EventSearchIndex;
import fr.soft_labs.eventbyte.event.infrastructure.web.EventResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchEventsService {

    private final EventSearchIndex searchIndex;
    private final GetEventsByIdsService getEventsByIdsService;

    public SearchEventsService(EventSearchIndex searchIndex,
                               GetEventsByIdsService getEventsByIdsService) {
        this.searchIndex = searchIndex;
        this.getEventsByIdsService = getEventsByIdsService;
    }

    public List<EventResponse> execute(String query, double lat, double lng, double radiusKm) {
        List<String> ids = searchIndex.search(query, lat, lng, radiusKm);
        return getEventsByIdsService.execute(ids);
    }
}