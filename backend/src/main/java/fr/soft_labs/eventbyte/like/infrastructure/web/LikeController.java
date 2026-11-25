package fr.soft_labs.eventbyte.like.infrastructure.web;

import fr.soft_labs.eventbyte.like.application.EventNotFoundException;
import fr.soft_labs.eventbyte.like.application.LikeEventService;
import fr.soft_labs.eventbyte.like.application.UnlikeEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LikeController {

    private final LikeEventService likeEventService;
    private final UnlikeEventService unlikeEventService;

    public LikeController(LikeEventService likeEventService,
                          UnlikeEventService unlikeEventService) {
        this.likeEventService = likeEventService;
        this.unlikeEventService = unlikeEventService;
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
