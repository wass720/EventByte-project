package fr.soft_labs.eventbyte.like.application;

import fr.soft_labs.eventbyte.like.application.port.EventCatalog;
import fr.soft_labs.eventbyte.like.application.port.LikeRepository;
import fr.soft_labs.eventbyte.like.domain.Like;
import org.springframework.stereotype.Service;

/**
 * US-9 : un utilisateur connecté retire son like.
 * Retirer un like inexistant est sans effet (idempotent, comme le like).
 */
@Service
public class UnlikeEventService {

    private final LikeRepository likeRepository;
    private final EventCatalog eventCatalog;

    public UnlikeEventService(LikeRepository likeRepository, EventCatalog eventCatalog) {
        this.likeRepository = likeRepository;
        this.eventCatalog = eventCatalog;
    }

    public void execute(String userId, String eventId) {
        if (!eventCatalog.exists(eventId)) {
            throw new EventNotFoundException(eventId);
        }
        likeRepository.delete(new Like(userId, eventId));
    }
}
