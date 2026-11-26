package fr.soft_labs.eventbyte.event.domain;

/**
 * Un type de ticket mis en vente pour un événement (ex. "VIP").
 *
 * @param price    prix unitaire en centimes
 * @param quantity stock total mis en vente
 */
public record EventTicket(String id, String name, int price, int quantity) {

    public EventTicket {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Un ticket doit avoir un id");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Le prix d'un ticket ne peut pas être négatif : " + id);
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("La quantité d'un ticket ne peut pas être négative : " + id);
        }
    }
}
