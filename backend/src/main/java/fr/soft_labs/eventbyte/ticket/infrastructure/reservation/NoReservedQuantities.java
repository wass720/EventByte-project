package fr.soft_labs.eventbyte.ticket.infrastructure.reservation;

import fr.soft_labs.eventbyte.ticket.application.port.ReservedQuantities;
import org.springframework.stereotype.Component;

/**
 * Implémentation provisoire : aucune réservation n'existe tant que l'Epic 4
 * (POST /bookings, stock atomique Redis) n'est pas livrée.
 *
 * À SUPPRIMER quand l'Epic 4 fournit sa propre implémentation de ReservedQuantities
 * (sinon Spring trouvera deux beans et refusera de démarrer).
 */
@Component
public class NoReservedQuantities implements ReservedQuantities {

    @Override
    public int reservedQuantity(String ticketId) {
        return 0;
    }
}
