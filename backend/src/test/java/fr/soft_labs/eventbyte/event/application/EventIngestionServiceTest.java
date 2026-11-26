package fr.soft_labs.eventbyte.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.EventTicket;
import fr.soft_labs.eventbyte.event.domain.Venue;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * US-2 / US-19 : cas limites de l'ingestion des messages Strapi, en particulier les
 * messages reçus plusieurs fois (RabbitMQ livre "au moins une fois", l'outbox de Strapi
 * renvoie les messages non acquittés).
 */
class EventIngestionServiceTest {

    private InMemoryEventRepository repository;
    private InMemorySearchIndex searchIndex;
    private EventIngestionService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEventRepository();
        searchIndex = new InMemorySearchIndex();
        service = new EventIngestionService(repository, searchIndex);
    }

    private static Event concert(String title, EventTicket... tickets) {
        return new Event("evt-1", title, "Description", "cover.png",
                new Venue("Salle", "1 rue X", 48.85, 2.35),
                Instant.parse("2026-12-31T20:00:00Z"), List.of(tickets));
    }

    @Test
    void a_created_event_is_stored_and_indexed() {
        service.upsert(concert("Concert", new EventTicket("std", "Standard", 2000, 100)));

        assertThat(repository.findById("evt-1")).isPresent();
        assertThat(searchIndex.documents).containsKey("evt-1");
    }

    @Test
    void the_same_message_received_twice_creates_no_duplicate() {
        Event event = concert("Concert", new EventTicket("std", "Standard", 2000, 100));

        service.upsert(event);
        service.upsert(event);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById("evt-1").orElseThrow().tickets()).hasSize(1);
        assertThat(searchIndex.documents).hasSize(1);
    }

    @Test
    void an_update_replaces_the_previous_state() {
        service.upsert(concert("Concert", new EventTicket("std", "Standard", 2000, 100)));

        service.upsert(concert("Concert (complet)", new EventTicket("std", "Standard", 2500, 0)));

        Event stored = repository.findById("evt-1").orElseThrow();
        assertThat(stored.title()).isEqualTo("Concert (complet)");
        assertThat(stored.tickets()).containsExactly(new EventTicket("std", "Standard", 2500, 0));
        assertThat(searchIndex.documents.get("evt-1").title()).isEqualTo("Concert (complet)");
    }

    @Test
    void a_ticket_removed_in_strapi_disappears() {
        service.upsert(concert("Concert",
                new EventTicket("std", "Standard", 2000, 100),
                new EventTicket("vip", "VIP", 9000, 10)));

        service.upsert(concert("Concert", new EventTicket("std", "Standard", 2000, 100)));

        assertThat(repository.findById("evt-1").orElseThrow().tickets())
                .extracting(EventTicket::id)
                .containsExactly("std");
    }

    @Test
    void an_update_received_before_the_creation_still_creates_the_event() {
        // event.updated arrivé avant event.created (messages désordonnés) : upsert crée l'événement.
        service.upsert(concert("Concert", new EventTicket("std", "Standard", 2000, 100)));

        assertThat(repository.findById("evt-1")).isPresent();
    }

    @Test
    void a_deleted_event_is_removed_from_the_base_and_the_index() {
        service.upsert(concert("Concert", new EventTicket("std", "Standard", 2000, 100)));

        service.delete("evt-1");

        assertThat(repository.findById("evt-1")).isEmpty();
        assertThat(searchIndex.documents).doesNotContainKey("evt-1");
    }

    @Test
    void a_deletion_received_twice_is_harmless() {
        service.upsert(concert("Concert", new EventTicket("std", "Standard", 2000, 100)));

        service.delete("evt-1");

        assertThatCode(() -> service.delete("evt-1")).doesNotThrowAnyException();
        assertThat(repository.count()).isZero();
    }

    @Test
    void deleting_an_unknown_event_is_harmless() {
        assertThatCode(() -> service.delete("unknown")).doesNotThrowAnyException();
    }

    @Test
    void other_events_are_not_affected() {
        service.upsert(concert("Concert", new EventTicket("std", "Standard", 2000, 100)));
        service.upsert(new Event("evt-2", "Théâtre", null, null, null, null, List.of()));

        service.delete("evt-1");

        assertThat(repository.findById("evt-2")).isPresent();
        assertThat(searchIndex.documents).containsOnlyKeys("evt-2");
    }

    @Test
    void an_event_without_venue_is_indexed_without_coordinates() {
        service.upsert(new Event("evt-3", "En ligne", null, null, null, null, List.of()));

        assertThat(searchIndex.documents.get("evt-3").latitude()).isNull();
        assertThat(searchIndex.documents.get("evt-3").longitude()).isNull();
    }
}
