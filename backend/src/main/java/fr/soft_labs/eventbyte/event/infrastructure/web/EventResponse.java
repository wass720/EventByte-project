package fr.soft_labs.eventbyte.event.infrastructure.web;

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
}