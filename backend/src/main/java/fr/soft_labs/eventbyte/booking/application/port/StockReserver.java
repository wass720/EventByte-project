package fr.soft_labs.eventbyte.booking.application.port;

import fr.soft_labs.eventbyte.booking.domain.Booking;

/**
 * Port sortant : bloque et libère les places d'une réservation.
 *
 * Tout ou rien : si une seule ligne manque de stock, aucune place n'est bloquée.
 * Les implémentations doivent être atomiques : deux réservations simultanées
 * du dernier ticket ne peuvent pas réussir toutes les deux.
 */
public interface StockReserver {

    /** @return true si toutes les places ont été bloquées, false si le stock est insuffisant. */
    boolean tryReserve(Booking booking);

    /** Rend les places d'une réservation précédemment bloquée par tryReserve. */
    void release(Booking booking);
}
