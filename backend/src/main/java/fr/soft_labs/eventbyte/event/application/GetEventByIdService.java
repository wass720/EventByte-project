package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.application.port.EventRepository;
import fr.soft_labs.eventbyte.like.application.port.LikeRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** US-5 : détail d'un événement, avec son nombre de likes (US-8). */
@Service
public class GetEventByIdService {

    private final EventRepository eventRepository;
    private final LikeRepository likeRepository;

    public GetEventByIdService(EventRepository eventRepository, LikeRepository likeRepository) {
        this.eventRepository = eventRepository;
        this.likeRepository = likeRepository;
    }

    public Optional<EventDetails> execute(String id) {
        return eventRepository.findById(id)
                .map(event -> new EventDetails(event, likeRepository.countByEventId(event.id())));
    }
}
