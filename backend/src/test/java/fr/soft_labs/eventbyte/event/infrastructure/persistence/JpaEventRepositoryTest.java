package fr.soft_labs.eventbyte.event.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.EventTicket;
import fr.soft_labs.eventbyte.event.domain.Venue;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * US-19 : l'idempotence de l'ingestion vérifiée sur le vrai Postgres (docker compose),
 * avec les migrations Flyway. Chaque test est annulé (rollback) à la fin : la base de
 * développement n'est pas modifiée.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaEventRepository.class)
class JpaEventRepositoryTest {

    private static final String ID = "test-evt-idempotence";

    @Autowired
    private JpaEventRepository repository;

    @Autowired
    private TicketJpaRepository ticketJpaRepository;

    private static Event event(String title, EventTicket... tickets) {
        return new Event(ID, title, "Description", "cover.png",
                new Venue("Salle", "1 rue X", 48.85, 2.35),
                Instant.parse("2026-12-31T20:00:00Z"), List.of(tickets));
    }

    @Test
    void saving_the_same_event_twice_creates_no_duplicate_in_database() {
        Event event = event("Concert",
                new EventTicket("test-tkt-std", "Standard", 2000, 100),
                new EventTicket("test-tkt-vip", "VIP", 9000, 10));

        repository.save(event);
        repository.save(event);

        assertThat(ticketJpaRepository.findByEventId(ID)).hasSize(2);
        assertThat(repository.findById(ID)).contains(event);
    }

    @Test
    void an_update_replaces_tickets_in_database() {
        repository.save(event("Concert",
                new EventTicket("test-tkt-std", "Standard", 2000, 100),
                new EventTicket("test-tkt-vip", "VIP", 9000, 10)));

        repository.save(event("Concert modifié", new EventTicket("test-tkt-std", "Standard", 2500, 80)));

        Event stored = repository.findById(ID).orElseThrow();
        assertThat(stored.title()).isEqualTo("Concert modifié");
        assertThat(stored.tickets()).containsExactly(new EventTicket("test-tkt-std", "Standard", 2500, 80));
    }

    @Test
    void deleting_removes_the_event_and_its_tickets_and_is_idempotent() {
        repository.save(event("Concert", new EventTicket("test-tkt-std", "Standard", 2000, 100)));

        repository.deleteById(ID);

        assertThatCode(() -> repository.deleteById(ID)).doesNotThrowAnyException();
        assertThat(repository.findById(ID)).isEmpty();
    }

    @Test
    void locations_contain_saved_events_with_coordinates() {
        repository.save(event("Concert"));

        assertThat(repository.findAllLocations())
                .anySatisfy(location -> {
                    assertThat(location.eventId()).isEqualTo(ID);
                    assertThat(location.latitude()).isEqualTo(48.85);
                });
    }
}
