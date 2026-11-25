package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.infrastructure.web.EventResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GetEventsByIdsService {

    private final GetEventByIdService getEventByIdService;

    public GetEventsByIdsService(GetEventByIdService getEventByIdService) {
        this.getEventByIdService = getEventByIdService;
    }

    public List<EventResponse> execute(List<String> ids) {
        return ids.stream()
                .map(getEventByIdService::execute)
                .flatMap(Optional::stream)
                .toList();
    }
}