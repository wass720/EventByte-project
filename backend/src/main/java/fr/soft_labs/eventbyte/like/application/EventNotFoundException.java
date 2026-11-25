package fr.soft_labs.eventbyte.like.application;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(String eventId) {
        super("Événement introuvable : " + eventId);
    }
}
