package fr.soft_labs.eventbyte.like.application;

import static org.assertj.core.api.Assertions.assertThat;

import fr.soft_labs.eventbyte.like.domain.Like;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListLikedEventsServiceTest {

    private InMemoryLikeRepository likeRepository;
    private ListLikedEventsService service;

    @BeforeEach
    void setUp() {
        likeRepository = new InMemoryLikeRepository();
        service = new ListLikedEventsService(likeRepository);
    }

    @Test
    void returns_nothing_when_i_liked_nothing() {
        assertThat(service.execute("user-1")).isEmpty();
    }

    @Test
    void returns_only_my_own_likes() {
        likeRepository.save(new Like("user-1", "evt-1"));
        likeRepository.save(new Like("user-2", "evt-2"));
        likeRepository.save(new Like("user-1", "evt-3"));

        assertThat(service.execute("user-1")).containsExactlyInAnyOrder("evt-1", "evt-3");
        assertThat(service.execute("user-2")).containsExactly("evt-2");
    }

    @Test
    void most_recent_like_comes_first() {
        likeRepository.save(new Like("user-1", "evt-1"));
        likeRepository.save(new Like("user-1", "evt-2"));

        assertThat(service.execute("user-1")).containsExactly("evt-2", "evt-1");
    }

    @Test
    void an_unliked_event_is_no_longer_listed() {
        likeRepository.save(new Like("user-1", "evt-1"));
        likeRepository.save(new Like("user-1", "evt-2"));
        likeRepository.delete(new Like("user-1", "evt-1"));

        assertThat(service.execute("user-1")).containsExactly("evt-2");
    }
}
