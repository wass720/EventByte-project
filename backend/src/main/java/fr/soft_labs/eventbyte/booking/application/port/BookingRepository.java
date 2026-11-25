package fr.soft_labs.eventbyte.booking.application.port;

import fr.soft_labs.eventbyte.booking.domain.Booking;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Port sortant : persistance des réservations. */
public interface BookingRepository {

    /** Crée la réservation, ou met à jour son statut si elle existe déjà. */
    void save(Booking booking);

    Optional<Booking> findById(String bookingId);

    /** Les réservations d'un utilisateur, de la plus récente à la plus ancienne. */
    List<Booking> findByUserId(String userId);

    /** Réservations non payées dont le délai est dépassé, les plus anciennes d'abord. */
    List<Booking> findDueForExpiration(Instant now, int limit);

    /**
     * Passe la réservation en "expired" SEULEMENT si elle est encore non payée.
     * Atomique : si un paiement l'a confirmée entre-temps, rien n'est modifié.
     *
     * @return true si cet appel a fait expirer la réservation (c'est alors à l'appelant
     *         de rendre les places), false si elle avait déjà changé de statut
     */
    boolean markExpired(String bookingId);
}
