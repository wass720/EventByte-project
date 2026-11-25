package fr.soft_labs.eventbyte.like.application;

import fr.soft_labs.eventbyte.like.application.port.LikeRepository;
import fr.soft_labs.eventbyte.like.domain.Like;
import java.util.HashSet;
import java.util.Set;

/** Fake en mémoire du port LikeRepository, pour tester les use cases sans base. */
class InMemoryLikeRepository implements LikeRepository {

    private final Set<Like> likes = new HashSet<>();

    @Override
    public void save(Like like) {
        likes.add(like);
    }

    @Override
    public int countByEventId(String eventId) {
        return (int) likes.stream().filter(like -> like.eventId().equals(eventId)).count();
    }
}
