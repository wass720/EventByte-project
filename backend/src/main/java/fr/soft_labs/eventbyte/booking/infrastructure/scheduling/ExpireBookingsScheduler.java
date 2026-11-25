package fr.soft_labs.eventbyte.booking.infrastructure.scheduling;

import fr.soft_labs.eventbyte.booking.application.ExpireBookingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Déclenche périodiquement l'expiration des réservations non payées (US-14). */
@Component
public class ExpireBookingsScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpireBookingsScheduler.class);

    private final ExpireBookingsService expireBookingsService;

    public ExpireBookingsScheduler(ExpireBookingsService expireBookingsService) {
        this.expireBookingsService = expireBookingsService;
    }

    @Scheduled(fixedDelayString = "${eventbyte.booking.expiration-check-interval:10s}")
    public void expireDueBookings() {
        try {
            int expired = expireBookingsService.execute();
            if (expired > 0) {
                log.info("{} réservation(s) expirée(s), places rendues", expired);
            }
        } catch (RuntimeException e) {
            // Ne jamais laisser une erreur arrêter la tâche : elle est rejouée au prochain tour.
            log.error("Échec de l'expiration des réservations", e);
        }
    }
}
