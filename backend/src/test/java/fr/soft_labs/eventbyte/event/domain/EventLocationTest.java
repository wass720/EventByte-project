package fr.soft_labs.eventbyte.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class EventLocationTest {

    // Tour Eiffel et Notre-Dame de Paris : environ 4,1 km à vol d'oiseau.
    private final EventLocation notreDame = new EventLocation("evt-1", 48.8530, 2.3499);

    @Test
    void distance_to_the_same_point_is_zero() {
        assertThat(notreDame.distanceKmFrom(48.8530, 2.3499)).isCloseTo(0.0, within(0.001));
    }

    @Test
    void distance_between_two_known_places() {
        assertThat(notreDame.distanceKmFrom(48.8584, 2.2945)).isCloseTo(4.1, within(0.2));
    }

    @Test
    void paris_to_lyon_is_about_390_km() {
        EventLocation lyon = new EventLocation("evt-2", 45.7640, 4.8357);

        assertThat(lyon.distanceKmFrom(48.8566, 2.3522)).isCloseTo(392, within(5.0));
    }

    @Test
    void within_radius_includes_the_limit() {
        double distance = notreDame.distanceKmFrom(48.8584, 2.2945);

        assertThat(notreDame.isWithinKm(48.8584, 2.2945, distance)).isTrue();
        assertThat(notreDame.isWithinKm(48.8584, 2.2945, distance - 0.01)).isFalse();
    }
}
