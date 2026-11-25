package fr.soft_labs.eventbyte.like.domain;

/**
 * Un utilisateur aime un événement.
 *
 * Un like est identifié par le couple (userId, eventId) : un même utilisateur
 * ne peut liker un événement qu'une seule fois.
 *
 * Domaine pur : aucune dépendance à Spring ni à JPA.
 */
public record Like(String userId, String eventId) {

    public Like {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId est obligatoire");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId est obligatoire");
        }
    }
}
