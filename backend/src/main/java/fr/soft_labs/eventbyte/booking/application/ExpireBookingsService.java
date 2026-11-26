package fr.soft_labs.eventbyte.booking.application;

import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.application.port.PaymentGateway;
import fr.soft_labs.eventbyte.booking.application.port.StockReserver;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * US-14 : les réservations non payées expirent et leurs places retournent en vente.
 *
 * Appelé régulièrement par un scheduler. Le passage en "expired" est atomique
 * (BookingRepository.markExpired) : si deux exécutions ou un paiement se croisent,
 * une seule voit "true" et rend les places, donc jamais de double libération.
 * On change le statut AVANT de rendre les places : en cas de panne entre les deux,
 * on perd des places (rattrapable) au lieu d'en vendre deux fois.
 * US-16 : le paiement Stripe éventuel est annulé AVANT le passage en "expired" (voir cancelPayment).
 */
@Service
public class ExpireBookingsService {

    private static final Logger log = LoggerFactory.getLogger(ExpireBookingsService.class);
    private static final int BATCH_SIZE = 100;

    private final BookingRepository bookingRepository;
    private final StockReserver stockReserver;
    private final PaymentGateway paymentGateway;
    private final Clock clock;

    public ExpireBookingsService(BookingRepository bookingRepository,
                                 StockReserver stockReserver,
                                 PaymentGateway paymentGateway,
                                 Clock clock) {
        this.bookingRepository = bookingRepository;
        this.stockReserver = stockReserver;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
    }

    /** @return le nombre de réservations expirées par cet appel. */
    public int execute() {
        int expiredCount = 0;
        List<Booking> due;
        int expiredInBatch;
        do {
            due = bookingRepository.findDueForExpiration(clock.instant(), BATCH_SIZE);
            expiredInBatch = 0;
            for (Booking booking : due) {
                try {
                    if (!cancelPayment(booking)) {
                        continue; // réessayé au prochain passage, places toujours bloquées
                    }
                    if (bookingRepository.markExpired(booking.id())) {
                        stockReserver.release(booking);
                        expiredInBatch++;
                    }
                } catch (RuntimeException e) {
                    log.error("Expiration impossible pour la réservation {}", booking.id(), e);
                }
            }
            expiredCount += expiredInBatch;
            // Un lot sans aucun progrès (paiements impossibles à annuler) reviendrait à
            // l'identique : on s'arrête, le prochain passage du scheduler réessaiera.
        } while (due.size() == BATCH_SIZE && expiredInBatch > 0);
        return expiredCount;
    }

    /**
     * US-16 : une réservation "awaiting-payment" a un paiement Stripe ouvert. On l'annule AVANT
     * de la faire expirer : une fois annulé, plus personne ne peut payer les places rendues.
     *
     * Si l'annulation échoue (Stripe indisponible, ou paiement déjà effectué), la réservation
     * n'expire pas et garde ses places : mieux vaut les bloquer un peu plus longtemps que
     * vendre deux fois des places payées. Un paiement réussi sera confirmé par US-17.
     *
     * @return true si la réservation peut expirer
     */
    private boolean cancelPayment(Booking booking) {
        if (booking.payment() == null) {
            return true;
        }
        try {
            paymentGateway.cancelPayment(booking.payment().id());
            return true;
        } catch (RuntimeException e) {
            log.warn("Paiement {} de la réservation {} non annulable : elle n'expire pas pour l'instant",
                    booking.payment().id(), booking.id(), e);
            return false;
        }
    }
}
