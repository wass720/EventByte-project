package fr.soft_labs.eventbyte.booking.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Une réservation de tickets pour un seul événement.
 *
 * Immuable : chaque transition renvoie une nouvelle réservation. Une réservation naît
 * "requested", puis devient "reserved" (stock suffisant) ou "rejected" (stock insuffisant).
 * Une fois le paiement préparé (US-16) elle passe "awaiting-payment" ; si le paiement ne peut
 * pas être préparé, elle est "rejected". Tant qu'elle n'est pas payée, elle expire à
 * "expiresAt" (US-14) et passe "expired". La confirmation ("confirmed") arrive avec US-17.
 *
 * Montants en centimes : total = somme des lignes + frais. "payment" n'existe qu'à partir
 * de "awaiting-payment". Domaine pur : aucune dépendance à Spring ni à JPA.
 */
public record Booking(String id,
                      String userId,
                      String eventId,
                      Instant bookedAt,
                      Instant expiresAt,
                      List<BookingLine> lines,
                      BookingStatus status,
                      int total,
                      int fees,
                      BookingPayment payment) {

    /** Statuts où des places sont bloquées sans avoir été payées : ils peuvent expirer. */
    public static final Set<BookingStatus> UNPAID_STATUSES =
            Set.of(BookingStatus.RESERVED, BookingStatus.AWAITING_PAYMENT);

    public Booking {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Une réservation contient au moins une ligne");
        }
        if (status == BookingStatus.AWAITING_PAYMENT && payment == null) {
            throw new IllegalArgumentException("Une réservation en attente de paiement a un paiement");
        }
        lines = List.copyOf(lines);
    }

    /**
     * Nouvelle demande de réservation.
     *
     * @param paymentWindow temps laissé pour payer, à partir de bookedAt
     * @param feePerTicket  frais de service par ticket, en centimes
     */
    public static Booking request(String id, String userId, String eventId, Instant bookedAt,
                                  Duration paymentWindow, int feePerTicket, List<BookingLine> lines) {
        if (feePerTicket < 0) {
            throw new IllegalArgumentException("Les frais ne peuvent pas être négatifs");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Une réservation contient au moins une ligne");
        }
        int subtotal = lines.stream().mapToInt(BookingLine::subtotal).reduce(0, Math::addExact);
        int tickets = lines.stream().mapToInt(BookingLine::quantity).reduce(0, Math::addExact);
        int fees = Math.multiplyExact(tickets, feePerTicket);
        return new Booking(id, userId, eventId, bookedAt, bookedAt.plus(paymentWindow),
                lines, BookingStatus.REQUESTED, Math.addExact(subtotal, fees), fees, null);
    }

    /** Le stock a pu être bloqué. */
    public Booking reserve() {
        return transition(Set.of(BookingStatus.REQUESTED), BookingStatus.RESERVED, payment);
    }

    /** Le stock était insuffisant. */
    public Booking reject() {
        return transition(Set.of(BookingStatus.REQUESTED), BookingStatus.REJECTED, payment);
    }

    /** US-16 : le paiement est prêt, l'acheteur peut payer avec "payment.clientSecret". */
    public Booking awaitPayment(BookingPayment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Paiement manquant");
        }
        return transition(Set.of(BookingStatus.RESERVED), BookingStatus.AWAITING_PAYMENT, payment);
    }

    /** US-16 : le paiement n'a pas pu être préparé (prestataire indisponible, montant refusé...). */
    public Booking rejectPayment() {
        return transition(Set.of(BookingStatus.RESERVED), BookingStatus.REJECTED, payment);
    }

    /** Le délai de paiement est dépassé : les places repartent en vente. */
    public Booking expire() {
        return transition(UNPAID_STATUSES, BookingStatus.EXPIRED, payment);
    }

    /** Vrai si la réservation n'est pas payée et que son délai est atteint. */
    public boolean isDueForExpiration(Instant now) {
        return UNPAID_STATUSES.contains(status) && !now.isBefore(expiresAt);
    }

    private Booking transition(Set<BookingStatus> allowedFrom, BookingStatus target, BookingPayment newPayment) {
        if (!allowedFrom.contains(status)) {
            throw new IllegalStateException(
                    "Transition impossible de " + status.value() + " vers " + target.value());
        }
        return new Booking(id, userId, eventId, bookedAt, expiresAt, lines, target, total, fees, newPayment);
    }
}
