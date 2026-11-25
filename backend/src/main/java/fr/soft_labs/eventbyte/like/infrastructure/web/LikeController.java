package fr.soft_labs.eventbyte.like.infrastructure.web;

import fr.soft_labs.eventbyte.event.application.GetEventsByIdsService;
import fr.soft_labs.eventbyte.event.infrastructure.web.EventResponse;
import fr.soft_labs.eventbyte.like.application.EventNotFoundException;
import fr.soft_labs.eventbyte.like.application.LikeEventService;
import fr.soft_labs.eventbyte.like.application.ListLikedEventsService;
import fr.soft_labs.eventbyte.like.application.UnlikeEventService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LikeController {

    private final LikeEventService likeEventService;
    private final UnlikeEventService unlikeEventService;
    private final ListLikedEventsService listLikedEventsService;
    private final GetEventsByIdsService getEventsByIdsService;

    public LikeController(LikeEventService likeEventService,
                          UnlikeEventService unlikeEventService,
                          ListLikedEventsService listLikedEventsService,
                          GetEventsByIdsService getEventsByIdsService) {
        this.likeEventService = likeEventService;
        this.unlikeEventService = unlikeEventService;
        this.listLikedEventsService = listLikedEventsService;
        this.getEventsByIdsService = getEventsByIdsService;
    }

    /**
     * US-10. Mes événements likés, au même format que GET /events (Epic 2).
     * Route littérale : Spring la préfère à GET /events/{id}.
     */
    @GetMapping("/events/liked")
    public List<EventResponse> liked(@AuthenticationPrincipal Jwt jwt) {
        List<String> likedEventIds = listLikedEventsService.execute(jwt.getSubject());
        return getEventsByIdsService.execute(likedEventIds);
    }

    /**
     * US-8. L'utilisateur est toujours celui du JWT ("sub"), jamais une valeur
     * envoyée par le client.
     */
    @PostMapping("/events/{eventId}/like")
    public ResponseEntity<Void> like(@PathVariable String eventId,
                                     @AuthenticationPrincipal Jwt jwt) {
        likeEventService.execute(jwt.getSubject(), eventId);
        return ResponseEntity.ok().build();
    }

    /** US-9. Ne retire que le like de l'utilisateur du JWT. */
    @DeleteMapping("/events/{eventId}/like")
    public ResponseEntity<Void> unlike(@PathVariable String eventId,
                                       @AuthenticationPrincipal Jwt jwt) {
        unlikeEventService.execute(jwt.getSubject(), eventId);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<Void> handleEventNotFound() {
        return ResponseEntity.notFound().build();
    }
}
