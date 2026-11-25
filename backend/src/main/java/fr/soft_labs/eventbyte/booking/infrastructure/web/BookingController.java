package fr.soft_labs.eventbyte.booking.infrastructure.web;

import fr.soft_labs.eventbyte.booking.application.CreateBookingService;
import fr.soft_labs.eventbyte.booking.application.CreateBookingService.Item;
import fr.soft_labs.eventbyte.booking.application.GetBookingService;
import fr.soft_labs.eventbyte.booking.application.InvalidBookingException;
import fr.soft_labs.eventbyte.booking.application.ListUserBookingsService;
import fr.soft_labs.eventbyte.booking.application.TicketNotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

    private final CreateBookingService createBookingService;
    private final GetBookingService getBookingService;
    private final ListUserBookingsService listUserBookingsService;

    public BookingController(CreateBookingService createBookingService,
                             GetBookingService getBookingService,
                             ListUserBookingsService listUserBookingsService) {
        this.createBookingService = createBookingService;
        this.getBookingService = getBookingService;
        this.listUserBookingsService = listUserBookingsService;
    }

    /**
     * US-15. Mes réservations. Route littérale : Spring la préfère à GET /bookings/{id}.
     */
    @GetMapping("/bookings/user")
    public List<BookingResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return listUserBookingsService.execute(jwt.getSubject()).stream()
                .map(BookingResponse::from)
                .toList();
    }

    /** US-15. 404 si elle n'existe pas ou n'est pas à moi (jamais la réservation d'un autre). */
    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> byId(@PathVariable String id,
                                                @AuthenticationPrincipal Jwt jwt) {
        return getBookingService.execute(jwt.getSubject(), id)
                .map(BookingResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
