package fr.soft_labs.eventbyte.like.application;

import fr.soft_labs.eventbyte.like.application.port.LikeRepository;
import fr.soft_labs.eventbyte.like.domain.Like;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Fake en mémoire du port LikeRepository, pour tester les use cases sans base. */
class InMemoryLikeRepository implements LikeRepository {

    private final Set<Like> likes = new LinkedHashSet<>();

    @Override
    public void save(Like like) {
        likes.add(like);
    }

    @Override
    public void delete(Like like) {
        likes.remove(like);
    }

    @Override
    public int countByEventId(String eventId) {
        return (int) likes.stream().filter(like -> like.eventId().equals(eventId)).count();
    }

    @Override
    public List<String> findEventIdsByUserId(String userId) {
        // L'ordre d'insertion simule liked_at : le dernier like en premier.
        List<String> ids = new ArrayList<>(likes.stream()
                .filter(like -> like.userId().equals(userId))
                .map(Like::eventId)
                .toList());
        Collections.reverse(ids);
        return ids;
    }
}
