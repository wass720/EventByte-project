package fr.soft_labs.eventbyte.ticket.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.soft_labs.eventbyte.shared.infrastructure.security.SecurityConfig;
import fr.soft_labs.eventbyte.ticket.application.GetEventTicketsService;
import fr.soft_labs.eventbyte.ticket.application.GetEventTicketsService.AvailableTicket;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TicketController.class)
@Import(SecurityConfig.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetEventTicketsService getEventTicketsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void tickets_are_public_and_follow_the_api_contract() throws Exception {
        when(getEventTicketsService.execute("evt-1")).thenReturn(Optional.of(List.of(
                new AvailableTicket("std", "evt-1", "Standard", 2000, 60))));

        // Pas de token : un visiteur doit pouvoir voir les tickets.
        mockMvc.perform(get("/events/evt-1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("std"))
                .andExpect(jsonPath("$[0].eventId").value("evt-1"))
                .andExpect(jsonPath("$[0].name").value("Standard"))
                .andExpect(jsonPath("$[0].price").value(2000))
                .andExpect(jsonPath("$[0].quantity").value(60));
    }

    @Test
    void unknown_event_returns_404() throws Exception {
        when(getEventTicketsService.execute("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/events/unknown/tickets"))
                .andExpect(status().isNotFound());
    }
}
