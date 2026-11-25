package fr.soft_labs.eventbyte.booking.domain;

/**
 * Une ligne de réservation : un type de ticket, une quantité, et le prix unitaire
 * (en centimes) figé au moment de la réservation.
 */
public record BookingLine(String ticketId, String name, int quantity, int unitPrice) {

    public BookingLine {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être strictement positive");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Le prix ne peut pas être négatif");
        }
    }

    public int subtotal() {
        return Math.multiplyExact(quantity, unitPrice);
    }
}
