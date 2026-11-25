package fr.soft_labs.eventbyte.like.application.port;

/**
 * Port sortant : permet au module "like" de savoir si un événement existe,
 * sans dépendre directement de la persistance du module "event".
 */
public interface EventCatalog {

    boolean exists(String eventId);
}
