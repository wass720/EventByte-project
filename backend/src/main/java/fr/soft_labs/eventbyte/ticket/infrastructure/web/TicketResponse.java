package fr.soft_labs.eventbyte.ticket.infrastructure.web;

import fr.soft_labs.eventbyte.ticket.application.GetEventTicketsService.AvailableTicket;

/**
 * DTO du contrat d'API (README) : prix en centimes, quantity = places disponibles.
 */
public record TicketResponse(String id, String eventId, String name, int price, int quantity) {

    static TicketResponse from(AvailableTicket ticket) {
        return new TicketResponse(ticket.id(), ticket.eventId(), ticket.name(),
                ticket.price(), ticket.quantity());
    }
}
