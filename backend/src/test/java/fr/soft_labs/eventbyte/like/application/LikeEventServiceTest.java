package fr.soft_labs.eventbyte.like.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LikeEventServiceTest {

    private static final String EVENT = "evt-1";

    private InMemoryLikeRepository likeRepository;
    private LikeEventService service;

    @BeforeEach
    void setUp() {
        likeRepository = new InMemoryLikeRepository();
        Set<String> existingEvents = Set.of(EVENT, "evt-2");
        service = new LikeEventService(likeRepository, existingEvents::contains);
    }

    @Test
    void liking_an_event_increments_its_liked_count() {
        service.execute("user-1", EVENT);

        assertThat(likeRepository.countByEventId(EVENT)).isEqualTo(1);
    }

    @Test
    void liking_twice_counts_as_a_single_like() {
        service.execute("user-1", EVENT);
        service.execute("user-1", EVENT);

        assertThat(likeRepository.countByEventId(EVENT)).isEqualTo(1);
    }

    @Test
    void each_user_adds_one_like() {
        service.execute("user-1", EVENT);
        service.execute("user-2", EVENT);

        assertThat(likeRepository.countByEventId(EVENT)).isEqualTo(2);
    }

    @Test
    void a_like_only_counts_for_its_own_event() {
        service.execute("user-1", EVENT);

        assertThat(likeRepository.countByEventId("evt-2")).isZero();
    }

    @Test
    void liking_an_unknown_event_fails_and_saves_nothing() {
        assertThatThrownBy(() -> service.execute("user-1", "unknown"))
                .isInstanceOf(EventNotFoundException.class);

        assertThat(likeRepository.countByEventId("unknown")).isZero();
    }
}
