package fr.soft_labs.eventbyte.event.domain;

/** Lieu d'un événement. Les coordonnées peuvent manquer si Strapi ne les a pas envoyées. */
public record Venue(String name, String address, Double latitude, Double longitude) {

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
}
