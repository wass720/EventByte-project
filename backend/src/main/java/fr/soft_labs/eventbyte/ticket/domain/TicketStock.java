package fr.soft_labs.eventbyte.ticket.domain;

/**
 * Un type de ticket d'un événement, avec son stock total (reçu de Strapi).
 *
 * Domaine pur : aucune dépendance à Spring ni à JPA.
 *
 * @param price         prix unitaire en centimes
 * @param totalQuantity nombre total de places mises en vente
 */
public record TicketStock(String id, String eventId, String name, int price, int totalQuantity) {

    /**
     * Places encore disponibles une fois retirées celles des réservations en cours.
     * Jamais négatif, même si les réservations dépassent le stock (ex. stock réduit
     * dans Strapi après des réservations).
     */
    public int availableQuantity(int reservedQuantity) {
        return Math.max(0, totalQuantity - Math.max(0, reservedQuantity));
    }
}
