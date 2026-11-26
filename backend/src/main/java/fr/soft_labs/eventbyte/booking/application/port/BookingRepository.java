package fr.soft_labs.eventbyte.booking.application.port;

import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Port sortant : persistance des réservations. */
public interface BookingRepository {

    /** Crée la réservation, ou met à jour son statut si elle existe déjà. */
    void save(Booking booking);

    /**
     * Enregistre le nouveau statut et le paiement de la réservation SEULEMENT si son statut
     * en base est encore "expectedStatus". Atomique (compare-and-set) : si elle a changé
     * entre-temps (expirée, par exemple), rien n'est modifié.
     *
     * @return true si cet appel a fait la mise à jour
     */
    boolean update(Booking booking, BookingStatus expectedStatus);

    Optional<Booking> findById(String bookingId);

    /** US-17 : la réservation payée par ce paiement (Stripe PaymentIntent id). */
    Optional<Booking> findByPaymentId(String paymentId);

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
