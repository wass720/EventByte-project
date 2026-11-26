package fr.soft_labs.eventbyte.booking.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.soft_labs.eventbyte.booking.application.CreateBookingService;
import fr.soft_labs.eventbyte.booking.application.CreateBookingService.Item;
import fr.soft_labs.eventbyte.booking.application.GetBookingService;
import fr.soft_labs.eventbyte.booking.application.InvalidBookingException;
import fr.soft_labs.eventbyte.booking.application.ListUserBookingsService;
import fr.soft_labs.eventbyte.booking.application.TicketNotFoundException;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import fr.soft_labs.eventbyte.booking.domain.BookingPayment;
import fr.soft_labs.eventbyte.shared.infrastructure.security.SecurityConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest {

    private static final String BODY = """
            { "items": [ { "id": "t-1", "quantity": 2 } ] }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBookingService createBookingService;

    @MockitoBean
    private GetBookingService getBookingService;

    @MockitoBean
    private ListUserBookingsService listUserBookingsService;

    // Évite de contacter Keycloak : le JWT est simulé par jwt().
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void create_without_token_returns_401() throws Exception {
        mockMvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(createBookingService);
    }

    @Test
    void create_uses_the_user_from_the_jwt_and_returns_the_booking_id() throws Exception {
        when(createBookingService.execute("user-1", List.of(new Item("t-1", 2)))).thenReturn("b-42");

        mockMvc.perform(post("/bookings")
                        .with(jwt().jwt(j -> j.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("b-42"));

        verify(createBookingService).execute("user-1", List.of(new Item("t-1", 2)));
    }

    @Test
    void the_user_in_the_body_is_ignored() throws Exception {
        when(createBookingService.execute(any(), any())).thenReturn("b-1");

        mockMvc.perform(post("/bookings")
                        .with(jwt().jwt(j -> j.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "userId": "hacker", "items": [ { "id": "t-1", "quantity": 1 } ] }
                                """))
                .andExpect(status().isOk());

        verify(createBookingService).execute("user-1", List.of(new Item("t-1", 1)));
    }

    @Test
    void an_invalid_booking_returns_400() throws Exception {
        when(createBookingService.execute(any(), any())).thenThrow(new InvalidBookingException("x"));

        mockMvc.perform(post("/bookings")
                        .with(jwt().jwt(j -> j.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void an_unknown_ticket_returns_404() throws Exception {
        when(createBookingService.execute(any(), any())).thenThrow(new TicketNotFoundException("t-9"));

        mockMvc.perform(post("/bookings")
                        .with(jwt().jwt(j -> j.subject("user-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound());
    }

    private static Booking booking(String id) {
        return Booking.request(id, "user-1", "evt-1", Instant.parse("2026-01-01T10:00:00Z"),
                Duration.ofMinutes(10), 30, List.of(new BookingLine("t-1", "Early Bird", 2, 2500))).reserve();
    }

    @Test
    void a_booking_awaiting_payment_exposes_its_stripe_client_secret_and_amounts_in_cents() throws Exception {
        Booking awaiting = booking("b-1").awaitPayment(new BookingPayment("pi_1", "pi_1_secret_abc"));
        when(getBookingService.execute("user-1", "b-1")).thenReturn(Optional.of(awaiting));

        mockMvc.perform(get("/bookings/b-1").with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("awaiting-payment"))
                .andExpect(jsonPath("$.clientSecret").value("pi_1_secret_abc"))
                .andExpect(jsonPath("$.total").value(5060))
                .andExpect(jsonPath("$.fees").value(60))
                .andExpect(jsonPath("$.lines[0].price").value(2500))
                // L'id du PaymentIntent reste côté serveur.
                .andExpect(jsonPath("$.payment").doesNotExist());
    }

    @Test
    void the_client_secret_is_hidden_once_the_booking_no_longer_awaits_payment() throws Exception {
        Booking expired = booking("b-1").awaitPayment(new BookingPayment("pi_1", "pi_1_secret_abc")).expire();
        when(listUserBookingsService.execute("user-1")).thenReturn(List.of(expired));

        mockMvc.perform(get("/bookings/user").with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("expired"))
                .andExpect(jsonPath("$[0].clientSecret").doesNotExist());
    }

    @Test
    void get_booking_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/bookings/b-1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/bookings/user")).andExpect(status().isUnauthorized());

        verifyNoInteractions(getBookingService, listUserBookingsService);
    }

    @Test
    void get_booking_returns_the_contract_format() throws Exception {
        when(getBookingService.execute("user-1", "b-1")).thenReturn(Optional.of(booking("b-1")));

        mockMvc.perform(get("/bookings/b-1").with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("b-1"))
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.bookedAt").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.status").value("reserved"))
                .andExpect(jsonPath("$.total").value(5060))
                .andExpect(jsonPath("$.fees").value(60))
                .andExpect(jsonPath("$.lines[0].id").value("t-1"))
                .andExpect(jsonPath("$.lines[0].name").value("Early Bird"))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.lines[0].price").value(2500))
                .andExpect(jsonPath("$.clientSecret").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void get_booking_unknown_or_not_mine_returns_404() throws Exception {
        when(getBookingService.execute("user-1", "b-9")).thenReturn(Optional.empty());

        mockMvc.perform(get("/bookings/b-9").with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void user_bookings_uses_the_jwt_user_and_is_not_mistaken_for_an_id() throws Exception {
        when(listUserBookingsService.execute("user-1"))
                .thenReturn(List.of(booking("b-2"), booking("b-1")));

        mockMvc.perform(get("/bookings/user").with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("b-2"));

        verifyNoInteractions(getBookingService);
    }
}
