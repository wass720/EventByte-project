package fr.soft_labs.eventbyte.booking.application.port;

import fr.soft_labs.eventbyte.booking.domain.Booking;

/**
 * Port sortant : bloque les places d'une réservation.
 *
 * Tout ou rien : si une seule ligne manque de stock, aucune place n'est bloquée.
 */
public interface StockReserver {

    /** @return true si toutes les places ont été bloquées, false si le stock est insuffisant. */
    boolean tryReserve(Booking booking);
}
