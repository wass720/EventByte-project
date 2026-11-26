package fr.soft_labs.eventbyte.event.infrastructure.web;

import fr.soft_labs.eventbyte.event.application.EventDetails;
import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.Venue;

/**
 * DTO du contrat d'API (README) : "adresse" (et non "address"), coordonnées imbriquées,
 * prix en centimes, startAt au format ISO 8601.
 */
public record EventResponse(
        String id,
        String title,
        String cover,
        int likedCount,
        Venue venue,
        boolean isSoldOut,
        String startAt,
        int lowestPrice,
        String description
) {
    public record Venue(String name, String adresse, Coordinates coordinates) {
    }

    public record Coordinates(Double latitude, Double longitude) {
    }

    public static EventResponse from(EventDetails details) {
        Event event = details.event();
        fr.soft_labs.eventbyte.event.domain.Venue venue = event.venue();
        return new EventResponse(
                event.id(),
                event.title(),
                event.cover(),
                details.likedCount(),
                venue == null ? null : new Venue(
                        venue.name(),
                        venue.address(),
                        new Coordinates(venue.latitude(), venue.longitude())),
                event.isSoldOut(),
                event.startAt() == null ? null : event.startAt().toString(),
                event.lowestPrice(),
                event.description());
    }
}
