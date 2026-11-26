package fr.soft_labs.eventbyte.event.application;

import fr.soft_labs.eventbyte.event.domain.Event;

/**
 * Ce que les use cases de lecture renvoient : l'événement et son nombre de likes.
 * La couche web le transforme en EventResponse (format du contrat d'API).
 */
public record EventDetails(Event event, int likedCount) {
}
