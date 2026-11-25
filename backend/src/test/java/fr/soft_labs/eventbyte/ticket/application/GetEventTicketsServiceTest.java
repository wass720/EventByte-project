package fr.soft_labs.eventbyte.ticket.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.soft_labs.eventbyte.ticket.application.GetEventTicketsService.AvailableTicket;
import fr.soft_labs.eventbyte.ticket.application.port.TicketCatalog;
import fr.soft_labs.eventbyte.ticket.domain.TicketStock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetEventTicketsServiceTest {

    private static final String EVENT = "evt-1";

    private final Map<String, Integer> reserved = new HashMap<>();
    private GetEventTicketsService service;

    @BeforeEach
    void setUp() {
        TicketCatalog catalog = new TicketCatalog() {
            @Override
            public boolean eventExists(String eventId) {
                return eventId.equals(EVENT) || eventId.equals("evt-empty");
            }

            @Override
            public List<TicketStock> findByEventId(String eventId) {
                if (!eventId.equals(EVENT)) {
                    return List.of();
                }
                return List.of(
                        new TicketStock("vip", EVENT, "VIP", 9000, 5),
                        new TicketStock("std", EVENT, "Standard", 2000, 100));
            }
        };
        service = new GetEventTicketsService(catalog, ticketId -> reserved.getOrDefault(ticketId, 0));
    }

    @Test
    void returns_name_price_and_quantity_of_each_ticket() {
        List<AvailableTicket> tickets = service.execute(EVENT).orElseThrow();

        assertThat(tickets).containsExactly(
                new AvailableTicket("std", EVENT, "Standard", 2000, 100),
                new AvailableTicket("vip", EVENT, "VIP", 9000, 5));
    }

    @Test
    void cheapest_ticket_comes_first() {
        assertThat(service.execute(EVENT).orElseThrow())
                .extracting(AvailableTicket::price)
                .containsExactly(2000, 9000);
    }

    @Test
    void quantity_accounts_for_reservations_in_progress() {
        reserved.put("std", 40);
        reserved.put("vip", 5);

        assertThat(service.execute(EVENT).orElseThrow())
                .extracting(AvailableTicket::id, AvailableTicket::quantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("std", 60),
                        org.assertj.core.groups.Tuple.tuple("vip", 0));
    }

    @Test
    void event_without_tickets_returns_an_empty_list() {
        assertThat(service.execute("evt-empty")).hasValue(List.of());
    }

    @Test
    void unknown_event_returns_nothing() {
        assertThat(service.execute("unknown")).isEmpty();
    }
}
