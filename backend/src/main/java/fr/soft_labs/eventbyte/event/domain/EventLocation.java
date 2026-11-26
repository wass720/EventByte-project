package fr.soft_labs.eventbyte.event.domain;

/**
 * Position d'un événement, sans le reste de ses données : suffit pour filtrer
 * par distance sans charger tous les tickets (US-4).
 */
public record EventLocation(String eventId, double latitude, double longitude) {

    public double distanceKmFrom(double lat, double lng) {
        return GeoDistance.betweenKm(lat, lng, latitude, longitude);
    }

    public boolean isWithinKm(double lat, double lng, double radiusKm) {
        return distanceKmFrom(lat, lng) <= radiusKm;
    }
}
