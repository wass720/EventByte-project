package fr.soft_labs.eventbyte.ticket.application;

import fr.soft_labs.eventbyte.ticket.application.port.ReservedQuantities;
import fr.soft_labs.eventbyte.ticket.application.port.TicketCatalog;
import fr.soft_labs.eventbyte.ticket.domain.TicketStock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * US-11 : un visiteur voit les types de tickets d'un événement, leur prix et
 * leur disponibilité. La quantité renvoyée tient compte des réservations en cours.
 */
@Service
public class GetEventTicketsService {

    private final TicketCatalog ticketCatalog;
    private final ReservedQuantities reservedQuantities;

    public GetEventTicketsService(TicketCatalog ticketCatalog, ReservedQuantities reservedQuantities) {
        this.ticketCatalog = ticketCatalog;
        this.reservedQuantities = reservedQuantities;
    }

    /** Vide si l'événement n'existe pas. */
    public Optional<List<AvailableTicket>> execute(String eventId) {
        if (!ticketCatalog.eventExists(eventId)) {
            return Optional.empty();
        }
        List<AvailableTicket> tickets = ticketCatalog.findByEventId(eventId).stream()
                .sorted(Comparator.comparingInt(TicketStock::price))
                .map(this::toAvailableTicket)
                .toList();
        return Optional.of(tickets);
    }

    private AvailableTicket toAvailableTicket(TicketStock ticket) {
        int reserved = reservedQuantities.reservedQuantity(ticket.id());
        return new AvailableTicket(ticket.id(), ticket.eventId(), ticket.name(), ticket.price(),
                ticket.availableQuantity(reserved));
    }

    public record AvailableTicket(String id, String eventId, String name, int price, int quantity) {
    }
}
