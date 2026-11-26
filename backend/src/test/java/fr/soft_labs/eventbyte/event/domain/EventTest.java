package fr.soft_labs.eventbyte.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventTest {

    private static Event eventWith(EventTicket... tickets) {
        return new Event("evt-1", "Concert", "Description", "cover.png",
                new Venue("Salle", "1 rue X", 48.85, 2.35), null, List.of(tickets));
    }

    @Test
    void lowest_price_is_the_cheapest_ticket() {
        Event event = eventWith(
                new EventTicket("vip", "VIP", 9000, 5),
                new EventTicket("std", "Standard", 2000, 100));

        assertThat(event.lowestPrice()).isEqualTo(2000);
    }

    @Test
    void lowest_price_counts_tickets_even_when_out_of_stock() {
        Event event = eventWith(
                new EventTicket("early", "Early bird", 1000, 0),
                new EventTicket("std", "Standard", 2000, 100));

        assertThat(event.lowestPrice()).isEqualTo(1000);
    }

    @Test
    void lowest_price_is_zero_without_tickets() {
        assertThat(eventWith().lowestPrice()).isZero();
    }

    @Test
    void sold_out_when_every_ticket_is_out_of_stock() {
        Event event = eventWith(
                new EventTicket("vip", "VIP", 9000, 0),
                new EventTicket("std", "Standard", 2000, 0));

        assertThat(event.isSoldOut()).isTrue();
    }

    @Test
    void not_sold_out_while_one_ticket_remains() {
        Event event = eventWith(
                new EventTicket("vip", "VIP", 9000, 0),
                new EventTicket("std", "Standard", 2000, 1));

        assertThat(event.isSoldOut()).isFalse();
    }

    @Test
    void an_event_without_tickets_is_sold_out() {
        assertThat(eventWith().isSoldOut()).isTrue();
    }

    @Test
    void missing_ticket_list_becomes_empty() {
        Event event = new Event("evt-1", "Concert", null, null, null, null, null);

        assertThat(event.tickets()).isEmpty();
    }

    @Test
    void ticket_list_cannot_be_changed_from_outside() {
        List<EventTicket> tickets = new ArrayList<>(List.of(new EventTicket("std", "Standard", 2000, 10)));
        Event event = new Event("evt-1", "Concert", null, null, null, null, tickets);

        tickets.clear();

        assertThat(event.tickets()).hasSize(1);
    }

    @Test
    void an_event_needs_an_id() {
        assertThatThrownBy(() -> new Event(" ", "Concert", null, null, null, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void a_ticket_cannot_have_a_negative_price_or_quantity() {
        assertThatThrownBy(() -> new EventTicket("std", "Standard", -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventTicket("std", "Standard", 1000, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void venue_without_coordinates_has_no_location() {
        assertThat(new Venue("Salle", "1 rue X", null, 2.35).hasLocation()).isFalse();
        assertThat(new Venue("Salle", "1 rue X", 48.85, 2.35).hasLocation()).isTrue();
    }
}
