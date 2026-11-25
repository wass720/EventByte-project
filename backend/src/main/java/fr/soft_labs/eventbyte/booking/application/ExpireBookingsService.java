package fr.soft_labs.eventbyte.booking.application;

import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
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
 */
@Service
public class ExpireBookingsService {

    private static final Logger log = LoggerFactory.getLogger(ExpireBookingsService.class);
    private static final int BATCH_SIZE = 100;

    private final BookingRepository bookingRepository;
    private final StockReserver stockReserver;
    private final Clock clock;

    public ExpireBookingsService(BookingRepository bookingRepository,
                                 StockReserver stockReserver,
                                 Clock clock) {
        this.bookingRepository = bookingRepository;
        this.stockReserver = stockReserver;
        this.clock = clock;
    }

    /** @return le nombre de réservations expirées par cet appel. */
    public int execute() {
        int expiredCount = 0;
        List<Booking> due;
        do {
            due = bookingRepository.findDueForExpiration(clock.instant(), BATCH_SIZE);
            for (Booking booking : due) {
                try {
                    if (bookingRepository.markExpired(booking.id())) {
                        stockReserver.release(booking);
                        expiredCount++;
                    }
                } catch (RuntimeException e) {
                    log.error("Expiration impossible pour la réservation {}", booking.id(), e);
                }
            }
        } while (due.size() == BATCH_SIZE);
        return expiredCount;
    }
}
