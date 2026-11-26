package fr.soft_labs.eventbyte.event.infrastructure.web;

import fr.soft_labs.eventbyte.event.application.DiscoverEventsService;
import fr.soft_labs.eventbyte.event.application.EventDetails;
import fr.soft_labs.eventbyte.event.application.GetEventByIdService;
import fr.soft_labs.eventbyte.event.application.GetEventsByIdsService;
import fr.soft_labs.eventbyte.event.application.SearchEventsService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController {

    private final GetEventByIdService getEventByIdService;
    private final GetEventsByIdsService getEventsByIdsService;
    private final DiscoverEventsService discoverEventsService;
    private final SearchEventsService searchEventsService;

    public EventController(GetEventByIdService getEventByIdService,
                           GetEventsByIdsService getEventsByIdsService,
                           DiscoverEventsService discoverEventsService,
                           SearchEventsService searchEventsService) {
        this.getEventByIdService = getEventByIdService;
        this.getEventsByIdsService = getEventsByIdsService;
        this.discoverEventsService = discoverEventsService;
        this.searchEventsService = searchEventsService;
    }

    @GetMapping("/events/discover")
    public List<EventResponse> discover(@RequestParam double lat,
                                        @RequestParam double lng) {
        return toResponses(discoverEventsService.execute(lat, lng));
    }

    @GetMapping("/events/search")
    public List<EventResponse> search(@RequestParam double lat,
                                      @RequestParam double lng,
                                      @RequestParam double radius,
                                      @RequestParam(defaultValue = "") String query) {
        return toResponses(searchEventsService.execute(query, lat, lng, radius));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable String id) {
        return getEventByIdService.execute(id)
                .map(EventResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/events")
    public List<EventResponse> getByIds(@RequestParam List<String> ids) {
        return toResponses(getEventsByIdsService.execute(ids));
    }

    private static List<EventResponse> toResponses(List<EventDetails> events) {
        return events.stream().map(EventResponse::from).toList();
    }
}
