package fr.soft_labs.eventbyte.like.infrastructure.web;

import fr.soft_labs.eventbyte.like.application.EventNotFoundException;
import fr.soft_labs.eventbyte.like.application.LikeEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LikeController {

    private final LikeEventService likeEventService;

    public LikeController(LikeEventService likeEventService) {
        this.likeEventService = likeEventService;
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

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<Void> handleEventNotFound() {
        return ResponseEntity.notFound().build();
    }
}
