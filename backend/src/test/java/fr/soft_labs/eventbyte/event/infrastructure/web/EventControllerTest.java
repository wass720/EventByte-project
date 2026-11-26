package fr.soft_labs.eventbyte.event.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.soft_labs.eventbyte.event.application.DiscoverEventsService;
import fr.soft_labs.eventbyte.event.application.EventDetails;
import fr.soft_labs.eventbyte.event.application.GetEventByIdService;
import fr.soft_labs.eventbyte.event.application.GetEventsByIdsService;
import fr.soft_labs.eventbyte.event.application.SearchEventsService;
import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.EventTicket;
import fr.soft_labs.eventbyte.event.domain.Venue;
import fr.soft_labs.eventbyte.shared.infrastructure.security.SecurityConfig;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Le JSON renvoyé respecte le contrat d'API du README (le frontend le valide avec zod). */
@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetEventByIdService getEventByIdService;

    @MockitoBean
    private GetEventsByIdsService getEventsByIdsService;

    @MockitoBean
    private DiscoverEventsService discoverEventsService;

    @MockitoBean
    private SearchEventsService searchEventsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final EventDetails CONCERT = new EventDetails(new Event(
            "evt-1", "Concert", "Description", "https://cdn/cover.png",
            new Venue("Salle", "1 rue X", 48.85, 2.35),
            Instant.parse("2026-12-31T20:00:00Z"),
            List.of(new EventTicket("vip", "VIP", 9000, 0), new EventTicket("std", "Standard", 2000, 5))),
            3);

    @Test
    void event_detail_follows_the_api_contract() throws Exception {
        when(getEventByIdService.execute("evt-1")).thenReturn(Optional.of(CONCERT));

        mockMvc.perform(get("/events/evt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("evt-1"))
                .andExpect(jsonPath("$.title").value("Concert"))
                .andExpect(jsonPath("$.cover").value("https://cdn/cover.png"))
                .andExpect(jsonPath("$.likedCount").value(3))
                .andExpect(jsonPath("$.venue.name").value("Salle"))
                .andExpect(jsonPath("$.venue.adresse").value("1 rue X"))
                .andExpect(jsonPath("$.venue.coordinates.latitude").value(48.85))
                .andExpect(jsonPath("$.venue.coordinates.longitude").value(2.35))
                .andExpect(jsonPath("$.isSoldOut").value(false))
                .andExpect(jsonPath("$.startAt").value("2026-12-31T20:00:00Z"))
                .andExpect(jsonPath("$.lowestPrice").value(2000))
                .andExpect(jsonPath("$.description").value("Description"));
    }

    @Test
    void unknown_event_returns_404() throws Exception {
        when(getEventByIdService.execute("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/events/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void discover_is_public_and_returns_a_list() throws Exception {
        when(discoverEventsService.execute(48.85, 2.35)).thenReturn(List.of(CONCERT));

        mockMvc.perform(get("/events/discover").param("lat", "48.85").param("lng", "2.35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("evt-1"));
    }

    @Test
    void events_by_ids_reads_a_comma_separated_list() throws Exception {
        when(getEventsByIdsService.execute(List.of("evt-1", "evt-2"))).thenReturn(List.of(CONCERT));

        mockMvc.perform(get("/events").param("ids", "evt-1,evt-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("evt-1"));
    }
}
