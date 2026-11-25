package fr.soft_labs.eventbyte.like.application;

import fr.soft_labs.eventbyte.like.application.port.EventCatalog;
import fr.soft_labs.eventbyte.like.application.port.LikeRepository;
import fr.soft_labs.eventbyte.like.domain.Like;
import org.springframework.stereotype.Service;

/**
 * US-8 : un utilisateur connecté like un événement.
 * Liker deux fois = un seul like (géré par LikeRepository.save, idempotent).
 */
@Service
public class LikeEventService {

    private final LikeRepository likeRepository;
    private final EventCatalog eventCatalog;

    public LikeEventService(LikeRepository likeRepository, EventCatalog eventCatalog) {
        this.likeRepository = likeRepository;
        this.eventCatalog = eventCatalog;
    }

    public void execute(String userId, String eventId) {
        if (!eventCatalog.exists(eventId)) {
            throw new EventNotFoundException(eventId);
        }
        likeRepository.save(new Like(userId, eventId));
    }
}
