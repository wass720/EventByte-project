package fr.soft_labs.eventbyte.booking.infrastructure.web;

import fr.soft_labs.eventbyte.booking.application.CreateBookingService;
import fr.soft_labs.eventbyte.booking.application.CreateBookingService.Item;
import fr.soft_labs.eventbyte.booking.application.InvalidBookingException;
import fr.soft_labs.eventbyte.booking.application.TicketNotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

    private final CreateBookingService createBookingService;

    public BookingController(CreateBookingService createBookingService) {
        this.createBookingService = createBookingService;
    }

    /**
     * US-12. L'utilisateur est celui du JWT ("sub"), jamais une valeur du corps.
     * Répond 200 { id } même si la réservation est rejetée : son statut se lit ensuite
     * avec GET /bookings/:id (US-15).
     */
    @PostMapping("/bookings")
    public Map<String, String> create(@RequestBody CreateBookingRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        List<Item> items = request.items() == null ? null : request.items().stream()
                .map(item -> item == null ? null : new Item(item.id(), item.quantity()))
                .toList();
        return Map.of("id", createBookingService.execute(jwt.getSubject(), items));
    }

    @ExceptionHandler(InvalidBookingException.class)
    public ResponseEntity<Void> handleInvalid() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Void> handleTicketNotFound() {
        return ResponseEntity.notFound().build();
    }
}
