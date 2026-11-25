package fr.soft_labs.eventbyte.ticket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TicketStockTest {

    private final TicketStock ticket = new TicketStock("tkt-1", "evt-1", "VIP", 9000, 10);

    @Test
    void without_reservations_everything_is_available() {
        assertThat(ticket.availableQuantity(0)).isEqualTo(10);
    }

    @Test
    void reservations_in_progress_are_subtracted() {
        assertThat(ticket.availableQuantity(3)).isEqualTo(7);
    }

    @Test
    void fully_reserved_ticket_has_no_availability() {
        assertThat(ticket.availableQuantity(10)).isZero();
    }

    @Test
    void availability_is_never_negative() {
        assertThat(ticket.availableQuantity(15)).isZero();
    }

    @Test
    void negative_reservation_count_is_ignored() {
        assertThat(ticket.availableQuantity(-2)).isEqualTo(10);
    }
}
