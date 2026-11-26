package fr.soft_labs.eventbyte.booking.application.port;

import fr.soft_labs.eventbyte.booking.domain.Booking;

/**
 * Port sortant (US-18) : prévient le propriétaire d'une réservation que son statut a changé
 * (SSE "booking_updated" en production).
 *
 * Appelé APRÈS l'enregistrement du changement. Les implémentations ne lèvent jamais
 * d'exception : une notification perdue ne doit pas annuler un changement de statut.
 */
public interface BookingNotifier {

    void bookingChanged(Booking booking);
}
