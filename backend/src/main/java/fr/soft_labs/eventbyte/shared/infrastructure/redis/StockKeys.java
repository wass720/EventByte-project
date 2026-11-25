package fr.soft_labs.eventbyte.shared.infrastructure.redis;

/**
 * Clés Redis du stock, partagées par les modules booking (écriture) et ticket (lecture).
 *
 * Redis ne stocke que les places BLOQUÉES par des réservations en cours
 * (reserved / awaiting-payment) : "reserved:{ticketId}" = nombre de places.
 * Le stock total reste dans la table tickets, alimentée par Strapi : une mise à jour
 * du stock dans Strapi n'a donc rien à resynchroniser dans Redis.
 */
public final class StockKeys {

    private StockKeys() {
    }

    public static String reserved(String ticketId) {
        return "reserved:" + ticketId;
    }
}
