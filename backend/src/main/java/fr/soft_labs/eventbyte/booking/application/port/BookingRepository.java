package fr.soft_labs.eventbyte.booking.application.port;

import fr.soft_labs.eventbyte.booking.domain.Booking;

/** Port sortant : persistance des réservations. */
public interface BookingRepository {

    /** Crée la réservation, ou met à jour son statut si elle existe déjà. */
    void save(Booking booking);
}
