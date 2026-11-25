package fr.soft_labs.eventbyte.like.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.soft_labs.eventbyte.like.application.port.EventCatalog;
import fr.soft_labs.eventbyte.like.domain.Like;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnlikeEventServiceTest {

    private static final String EVENT = "evt-1";

    private InMemoryLikeRepository likeRepository;
    private UnlikeEventService service;

    @BeforeEach
    void setUp() {
        likeRepository = new InMemoryLikeRepository();
        EventCatalog catalog = Set.of(EVENT)::contains;
        service = new UnlikeEventService(likeRepository, catalog);
    }

    @Test
    void unliking_removes_the_like() {
        likeRepository.save(new Like("user-1", EVENT));

        service.execute("user-1", EVENT);

        assertThat(likeRepository.countByEventId(EVENT)).isZero();
    }

    @Test
    void unliking_only_removes_my_own_like() {
        likeRepository.save(new Like("user-1", EVENT));
        likeRepository.save(new Like("user-2", EVENT));

        service.execute("user-1", EVENT);

        assertThat(likeRepository.countByEventId(EVENT)).isEqualTo(1);
    }

    @Test
    void unliking_an_event_i_did_not_like_does_nothing() {
        likeRepository.save(new Like("user-2", EVENT));

        service.execute("user-1", EVENT);

        assertThat(likeRepository.countByEventId(EVENT)).isEqualTo(1);
    }

    @Test
    void unliking_twice_is_harmless() {
        likeRepository.save(new Like("user-1", EVENT));

        service.execute("user-1", EVENT);
        service.execute("user-1", EVENT);

        assertThat(likeRepository.countByEventId(EVENT)).isZero();
    }

    @Test
    void unliking_an_unknown_event_fails() {
        assertThatThrownBy(() -> service.execute("user-1", "unknown"))
                .isInstanceOf(EventNotFoundException.class);
    }
}
