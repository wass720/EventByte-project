package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.application.port.EventRepository;
import fr.soft_labs.eventbyte.event.domain.EventLocation;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** US-4 : événements dans le rayon configuré autour de l'utilisateur, du plus proche au plus loin. */
@Service
public class DiscoverEventsService {

    private final EventRepository eventRepository;
    private final GetEventsByIdsService getEventsByIdsService;
    private final DiscoverProperties properties;

    public DiscoverEventsService(EventRepository eventRepository,
                                 GetEventsByIdsService getEventsByIdsService,
                                 DiscoverProperties properties) {
        this.eventRepository = eventRepository;
        this.getEventsByIdsService = getEventsByIdsService;
        this.properties = properties;
    }

    public List<EventDetails> execute(double lat, double lng) {
        List<String> nearbyIds = eventRepository.findAllLocations().stream()
                .filter(location -> location.isWithinKm(lat, lng, properties.radiusKm()))
                .sorted(Comparator.comparingDouble(location -> location.distanceKmFrom(lat, lng)))
                .map(EventLocation::eventId)
                .toList();

        return getEventsByIdsService.execute(nearbyIds);
    }
}
