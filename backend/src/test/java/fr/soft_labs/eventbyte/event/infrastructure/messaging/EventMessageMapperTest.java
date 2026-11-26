package fr.soft_labs.eventbyte.event.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.EventTicket;
import fr.soft_labs.eventbyte.event.domain.Venue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Traduction d'un message Strapi en domaine (les prix sont testés dans EventMessageMapperPriceTest). */
class EventMessageMapperTest {

    @Test
    void a_complete_message_becomes_a_domain_event() {
        EventMessage message = new EventMessage("evt-1", "Concert", "Description", "cover.png",
                "2026-12-31T20:00:00.000Z",
                new EventMessage.Venue("Salle", "1 rue X", 48.85, 2.35),
                List.of(new EventMessage.Ticket("std", "Standard", 100, new BigDecimal("25.50"))));

        Event event = EventMessageMapper.toDomain(message);

        assertThat(event.id()).isEqualTo("evt-1");
        assertThat(event.title()).isEqualTo("Concert");
        assertThat(event.startAt()).isEqualTo(Instant.parse("2026-12-31T20:00:00Z"));
        assertThat(event.venue()).isEqualTo(new Venue("Salle", "1 rue X", 48.85, 2.35));
        assertThat(event.tickets()).containsExactly(new EventTicket("std", "Standard", 2550, 100));
    }

    @Test
    void optional_fields_may_be_missing() {
        EventMessage message = new EventMessage("evt-1", "Concert", null, null, null, null, null);

        Event event = EventMessageMapper.toDomain(message);

        assertThat(event.venue()).isNull();
        assertThat(event.startAt()).isNull();
        assertThat(event.tickets()).isEmpty();
    }

    @Test
    void a_ticket_without_price_is_refused() {
        EventMessage message = new EventMessage("evt-1", "Concert", null, null, null, null,
                List.of(new EventMessage.Ticket("std", "Standard", 100, null)));

        assertThatThrownBy(() -> EventMessageMapper.toDomain(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("std");
    }
}
