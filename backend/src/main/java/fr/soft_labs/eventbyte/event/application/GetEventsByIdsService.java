package fr.soft_labs.eventbyte.event.application;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** US-6 : plusieurs événements par IDs, dans l'ordre demandé. Les IDs inconnus sont ignorés. */
@Service
public class GetEventsByIdsService {

    private final GetEventByIdService getEventByIdService;

    public GetEventsByIdsService(GetEventByIdService getEventByIdService) {
        this.getEventByIdService = getEventByIdService;
    }

    public List<EventDetails> execute(List<String> ids) {
        return ids.stream()
                .map(getEventByIdService::execute)
                .flatMap(Optional::stream)
                .toList();
    }
}
