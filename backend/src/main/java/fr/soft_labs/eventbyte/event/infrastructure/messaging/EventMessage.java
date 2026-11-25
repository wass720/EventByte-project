package fr.soft_labs.eventbyte.event.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventMessage(
        String id,
        String title,
        String description,
        String cover,
        Venue venue,
        List<Ticket> tickets
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Venue(String name, String address, Double latitude, Double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ticket(String id, String name, Integer quantity, Integer price) {
    }
}