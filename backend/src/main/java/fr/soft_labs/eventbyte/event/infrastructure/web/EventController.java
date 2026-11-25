package fr.soft_labs.eventbyte.event.infrastructure.web;

import fr.soft_labs.eventbyte.event.application.GetEventByIdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController {

    private final GetEventByIdService getEventByIdService;

    public EventController(GetEventByIdService getEventByIdService) {
        this.getEventByIdService = getEventByIdService;
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable String id) {
        return getEventByIdService.execute(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}