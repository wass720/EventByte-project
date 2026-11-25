package fr.soft_labs.eventbyte.like.application;

import fr.soft_labs.eventbyte.like.application.port.LikeRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * US-10 : un utilisateur connecté liste les événements qu'il a likés.
 * Ne renvoie que ses propres likes, du plus récent au plus ancien.
 */
@Service
public class ListLikedEventsService {

    private final LikeRepository likeRepository;

    public ListLikedEventsService(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    public List<String> execute(String userId) {
        return likeRepository.findEventIdsByUserId(userId);
    }
}
