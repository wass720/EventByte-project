package fr.soft_labs.eventbyte.like.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LikeTest {

    @Test
    void two_likes_with_same_user_and_event_are_equal() {
        assertThat(new Like("user-1", "evt-1")).isEqualTo(new Like("user-1", "evt-1"));
    }

    @Test
    void user_is_required() {
        assertThatThrownBy(() -> new Like(" ", "evt-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Like(null, "evt-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void event_is_required() {
        assertThatThrownBy(() -> new Like("user-1", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Like("user-1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
