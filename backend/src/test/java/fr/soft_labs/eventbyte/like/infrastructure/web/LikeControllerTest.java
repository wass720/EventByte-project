package fr.soft_labs.eventbyte.like.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.soft_labs.eventbyte.like.application.EventNotFoundException;
import fr.soft_labs.eventbyte.like.application.LikeEventService;
import fr.soft_labs.eventbyte.like.application.UnlikeEventService;
import fr.soft_labs.eventbyte.shared.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LikeController.class)
@Import(SecurityConfig.class)
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LikeEventService likeEventService;

    @MockitoBean
    private UnlikeEventService unlikeEventService;

    // Évite de contacter Keycloak : le JWT est simulé par jwt().
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void like_without_token_returns_401() throws Exception {
        mockMvc.perform(post("/events/evt-1/like"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(likeEventService);
    }

    @Test
    void like_uses_the_user_from_the_jwt() throws Exception {
        mockMvc.perform(post("/events/evt-1/like").with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isOk());

        verify(likeEventService).execute("user-1", "evt-1");
    }

    @Test
    void like_unknown_event_returns_404() throws Exception {
        doThrow(new EventNotFoundException("unknown"))
                .when(likeEventService).execute(any(), any());

        mockMvc.perform(post("/events/unknown/like").with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unlike_without_token_returns_401() throws Exception {
        mockMvc.perform(delete("/events/evt-1/like"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(unlikeEventService);
    }

    @Test
    void unlike_uses_the_user_from_the_jwt() throws Exception {
        mockMvc.perform(delete("/events/evt-1/like").with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isOk());

        verify(unlikeEventService).execute("user-1", "evt-1");
    }

    @Test
    void unlike_unknown_event_returns_404() throws Exception {
        doThrow(new EventNotFoundException("unknown"))
                .when(unlikeEventService).execute(any(), any());

        mockMvc.perform(delete("/events/unknown/like").with(jwt().jwt(j -> j.subject("user-1"))))
                .andExpect(status().isNotFound());
    }
}
