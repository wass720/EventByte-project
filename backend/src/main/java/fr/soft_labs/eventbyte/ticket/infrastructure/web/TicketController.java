package fr.soft_labs.eventbyte.ticket.infrastructure.web;

import fr.soft_labs.eventbyte.ticket.application.GetEventTicketsService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {

    private final GetEventTicketsService getEventTicketsService;

    public TicketController(GetEventTicketsService getEventTicketsService) {
        this.getEventTicketsService = getEventTicketsService;
    }

    /** US-11. Route publique : un visiteur non connecté peut voir les tickets. */
    @GetMapping("/events/{eventId}/tickets")
    public ResponseEntity<List<TicketResponse>> getTickets(@PathVariable String eventId) {
        return getEventTicketsService.execute(eventId)
                .map(tickets -> tickets.stream().map(TicketResponse::from).toList())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
