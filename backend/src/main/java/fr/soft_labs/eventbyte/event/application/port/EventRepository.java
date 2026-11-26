package fr.soft_labs.eventbyte.event.application.port;

import fr.soft_labs.eventbyte.event.domain.Event;
import fr.soft_labs.eventbyte.event.domain.EventLocation;
import java.util.List;
import java.util.Optional;

/**
 * Port sortant : stockage des événements et de leurs tickets.
 */
public interface EventRepository {

    /**
     * Crée l'événement ou le remplace entièrement s'il existe déjà, tickets compris
     * (un ticket absent du nouvel état disparaît). Idempotent : enregistrer deux fois
     * le même événement ne crée aucun doublon.
     */
    void save(Event event);

    /** Supprime l'événement et ses tickets. Sans effet s'il n'existe pas. */
    void deleteById(String eventId);

    Optional<Event> findById(String eventId);

    /** Positions de tous les événements qui ont des coordonnées. */
    List<EventLocation> findAllLocations();
}
