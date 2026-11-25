package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.domain.GeoDistance;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.EventJpaRepository;
import fr.soft_labs.eventbyte.event.infrastructure.web.EventResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DiscoverEventsService {

    private final EventJpaRepository eventRepository;
    private final GetEventsByIdsService getEventsByIdsService;
    private final DiscoverProperties properties;

    public DiscoverEventsService(EventJpaRepository eventRepository,
                                 GetEventsByIdsService getEventsByIdsService,
                                 DiscoverProperties properties) {
        this.eventRepository = eventRepository;
        this.getEventsByIdsService = getEventsByIdsService;
        this.properties = properties;
    }

    public List<EventResponse> execute(double lat, double lng) {
        List<String> nearbyIds = eventRepository.findAll().stream()
                .filter(event -> event.getLatitude() != null && event.getLongitude() != null)
                .map(event -> new NearbyEvent(
                        event.getId(),
                        GeoDistance.betweenKm(lat, lng, event.getLatitude(), event.getLongitude())))
                .filter(nearby -> nearby.distanceKm() <= properties.radiusKm())
                .sorted(Comparator.comparingDouble(NearbyEvent::distanceKm))
                .map(NearbyEvent::id)
                .toList();

        return getEventsByIdsService.execute(nearbyIds);
    }

    private record NearbyEvent(String id, double distanceKm) {
    }
}