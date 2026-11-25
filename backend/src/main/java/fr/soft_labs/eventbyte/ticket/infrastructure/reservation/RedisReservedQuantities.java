package fr.soft_labs.eventbyte.ticket.infrastructure.reservation;

import fr.soft_labs.eventbyte.shared.infrastructure.redis.StockKeys;
import fr.soft_labs.eventbyte.ticket.application.port.ReservedQuantities;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Places bloquées par les réservations en cours, lues dans Redis (écrites par
 * RedisStockReserver, module booking). Remplace NoReservedQuantities : la quantité
 * renvoyée par GET /events/:id/tickets (US-11) tient maintenant compte des réservations.
 */
@Component
public class RedisReservedQuantities implements ReservedQuantities {

    private final StringRedisTemplate redis;

    public RedisReservedQuantities(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public int reservedQuantity(String ticketId) {
        String value = redis.opsForValue().get(StockKeys.reserved(ticketId));
        return value == null ? 0 : Integer.parseInt(value);
    }
}
