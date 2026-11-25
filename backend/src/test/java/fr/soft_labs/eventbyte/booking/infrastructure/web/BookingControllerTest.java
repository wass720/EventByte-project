package fr.soft_labs.eventbyte.booking.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.soft_labs.eventbyte.booking.application.CreateBookingService;
import fr.soft_labs.eventbyte.booking.application.CreateBookingService.Item;
import fr.soft_labs.eventbyte.booking.application.InvalidBookingException;
import fr.soft_labs.eventbyte.booking.application.TicketNotFoundException;
import fr.soft_labs.eventbyte.shared.infrastructure.security.SecurityConfig;
import java.util.List;
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
}
