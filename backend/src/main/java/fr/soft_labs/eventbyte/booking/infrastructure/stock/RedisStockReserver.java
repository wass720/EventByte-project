package fr.soft_labs.eventbyte.booking.infrastructure.stock;

import fr.soft_labs.eventbyte.booking.application.port.StockReserver;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketEntity;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketJpaRepository;
import fr.soft_labs.eventbyte.shared.infrastructure.redis.StockKeys;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * US-13 : blocage atomique du stock dans Redis.
 *
 * Un script Lua s'exécute d'un seul bloc dans Redis (aucune autre commande ne s'intercale) :
 * il vérifie toutes les lignes PUIS les incrémente. Deux réservations simultanées du dernier
 * ticket sont donc sérialisées par Redis et une seule réussit. Pas de "lire puis écrire"
 * côté Java, qui laisserait passer les deux.
 *
 * Redis compte les places bloquées ; le total vient de la table tickets (voir StockKeys).
 */
@Component
public class RedisStockReserver implements StockReserver {

    /**
     * KEYS[i] = clé des places bloquées du ticket i ; ARGV[2i-1] = stock total, ARGV[2i] = quantité.
     * Renvoie 1 si tout a été bloqué, 0 sinon (rien n'est alors modifié).
     */
    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>("""
            for i = 1, #KEYS do
              local reserved = tonumber(redis.call('GET', KEYS[i]) or '0')
              local total = tonumber(ARGV[2 * i - 1])
              local quantity = tonumber(ARGV[2 * i])
              if total - reserved < quantity then
                return 0
              end
            end
            for i = 1, #KEYS do
              redis.call('INCRBY', KEYS[i], ARGV[2 * i])
            end
            return 1
            """, Long.class);

    /** KEYS[i] = clé du ticket i ; ARGV[i] = quantité à rendre. Ne descend jamais sous 0. */
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            for i = 1, #KEYS do
              local value = tonumber(redis.call('GET', KEYS[i]) or '0') - tonumber(ARGV[i])
              if value < 0 then
                value = 0
              end
              redis.call('SET', KEYS[i], value)
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final TicketJpaRepository ticketRepository;

    public RedisStockReserver(StringRedisTemplate redis, TicketJpaRepository ticketRepository) {
        this.redis = redis;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public boolean tryReserve(Booking booking) {
        List<BookingLine> lines = sortedLines(booking);
        Map<String, Integer> totals = ticketRepository
                .findAllById(lines.stream().map(BookingLine::ticketId).toList()).stream()
                .collect(Collectors.toMap(TicketEntity::getId, TicketEntity::getQuantity));

        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();
        for (BookingLine line : lines) {
            Integer total = totals.get(line.ticketId());
            if (total == null) {
                return false; // ticket supprimé dans Strapi entre-temps
            }
            keys.add(StockKeys.reserved(line.ticketId()));
            args.add(String.valueOf(total));
            args.add(String.valueOf(line.quantity()));
        }
        Long result = redis.execute(RESERVE, keys, args.toArray());
        return Long.valueOf(1).equals(result);
    }

    @Override
    public void release(Booking booking) {
        List<BookingLine> lines = sortedLines(booking);
        redis.execute(RELEASE,
                lines.stream().map(line -> StockKeys.reserved(line.ticketId())).toList(),
                lines.stream().map(line -> String.valueOf(line.quantity())).toArray());
    }

    private static List<BookingLine> sortedLines(Booking booking) {
        return booking.lines().stream().sorted(Comparator.comparing(BookingLine::ticketId)).toList();
    }
}
