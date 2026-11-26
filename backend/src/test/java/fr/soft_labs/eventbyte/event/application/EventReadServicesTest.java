package fr.soft_labs.eventbyte.event.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.Venue;
import fr.soft_labs.eventbyte.like.application.port.LikeRepository;
import fr.soft_labs.eventbyte.like.domain.Like;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US-4, US-5, US-6, US-7 : use cases de lecture des événements. */
class EventReadServicesTest {

    // Paris (centre) et des lieux à distance connue.
    private static final double PARIS_LAT = 48.8566;
    private static final double PARIS_LNG = 2.3522;

    private InMemoryEventRepository repository;
    private InMemorySearchIndex searchIndex;
    private final Map<String, Integer> likes = new HashMap<>();

    private GetEventByIdService getById;
    private GetEventsByIdsService getByIds;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEventRepository();
        searchIndex = new InMemorySearchIndex();
        getById = new GetEventByIdService(repository, new LikeCounts(likes));
        getByIds = new GetEventsByIdsService(getById);
    }

    private void event(String id, Double lat, Double lng) {
        repository.save(new Event(id, "Titre " + id, null, null,
                new Venue("Lieu", "Adresse", lat, lng), null, List.of()));
    }

    // --- US-5 : détail ---

    @Test
    void an_event_comes_with_its_like_count() {
        event("evt-1", PARIS_LAT, PARIS_LNG);
        likes.put("evt-1", 3);

        EventDetails details = getById.execute("evt-1").orElseThrow();

        assertThat(details.event().id()).isEqualTo("evt-1");
        assertThat(details.likedCount()).isEqualTo(3);
    }

    @Test
    void an_unknown_event_is_not_found() {
        assertThat(getById.execute("unknown")).isEmpty();
    }

    // --- US-6 : par IDs ---

    @Test
    void events_by_ids_keep_the_requested_order_and_skip_unknown_ids() {
        event("evt-1", PARIS_LAT, PARIS_LNG);
        event("evt-2", PARIS_LAT, PARIS_LNG);

        assertThat(getByIds.execute(List.of("evt-2", "unknown", "evt-1")))
                .extracting(details -> details.event().id())
                .containsExactly("evt-2", "evt-1");
    }

    // --- US-4 : découvrir près de moi ---

    @Test
    void discover_returns_events_within_the_radius_closest_first() {
        event("versailles", 48.8049, 2.1204);   // ~17 km
        event("notre-dame", 48.8530, 2.3499);   // ~0,4 km
        event("lyon", 45.7640, 4.8357);         // ~390 km
        DiscoverEventsService discover = new DiscoverEventsService(
                repository, getByIds, new DiscoverProperties(50));

        assertThat(discover.execute(PARIS_LAT, PARIS_LNG))
                .extracting(details -> details.event().id())
                .containsExactly("notre-dame", "versailles");
    }

    @Test
    void discover_ignores_events_without_coordinates() {
        event("sans-coordonnees", null, null);
        event("notre-dame", 48.8530, 2.3499);
        DiscoverEventsService discover = new DiscoverEventsService(
                repository, getByIds, new DiscoverProperties(50));

        assertThat(discover.execute(PARIS_LAT, PARIS_LNG))
                .extracting(details -> details.event().id())
                .containsExactly("notre-dame");
    }

    // --- US-7 : recherche ---

    @Test
    void search_returns_the_index_results_in_the_index_order() {
        event("evt-1", PARIS_LAT, PARIS_LNG);
        event("evt-2", PARIS_LAT, PARIS_LNG);
        searchIndex.searchResult.addAll(List.of("evt-2", "evt-1", "deleted-meanwhile"));
        SearchEventsService search = new SearchEventsService(searchIndex, getByIds);

        assertThat(search.execute("concert", PARIS_LAT, PARIS_LNG, 10))
                .extracting(details -> details.event().id())
                .containsExactly("evt-2", "evt-1");
    }

    /** Port LikeRepository réduit au comptage, seul besoin des lectures d'événements. */
    private record LikeCounts(Map<String, Integer> counts) implements LikeRepository {
        @Override
        public void save(Like like) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(Like like) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int countByEventId(String eventId) {
            return counts.getOrDefault(eventId, 0);
        }

        @Override
        public List<String> findEventIdsByUserId(String userId) {
            throw new UnsupportedOperationException();
        }
    }
}
