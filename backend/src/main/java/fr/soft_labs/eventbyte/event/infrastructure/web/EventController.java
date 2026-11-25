package fr.soft_labs.eventbyte.event.infrastructure.web;

import fr.soft_labs.eventbyte.event.application.GetEventByIdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import fr.soft_labs.eventbyte.event.application.GetEventsByIdsService;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class EventController {

    private final GetEventByIdService getEventByIdService;
    private final GetEventsByIdsService getEventsByIdsService;


    public EventController(GetEventByIdService getEventByIdService,
                           GetEventsByIdsService getEventsByIdsService) {
        this.getEventByIdService = getEventByIdService;
        this.getEventsByIdsService = getEventsByIdsService;

    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable String id) {
        return getEventByIdService.execute(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/events")
    public List<EventResponse> getByIds(@RequestParam List<String> ids) {
        return getEventsByIdsService.execute(ids);
    }
}