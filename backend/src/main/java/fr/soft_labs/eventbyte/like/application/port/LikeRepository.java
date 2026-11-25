package fr.soft_labs.eventbyte.like.application.port;

import fr.soft_labs.eventbyte.like.domain.Like;

/**
 * Port sortant : stockage des likes.
 */
public interface LikeRepository {

    /**
     * Enregistre le like s'il n'existe pas encore. Idempotent : liker deux fois
     * ne crée qu'un seul like, y compris en cas de requêtes simultanées.
     */
    void save(Like like);

    /**
     * Supprime le like s'il existe. Idempotent : sans effet si l'utilisateur
     * n'avait pas liké l'événement.
     */
    void delete(Like like);

    int countByEventId(String eventId);
}
