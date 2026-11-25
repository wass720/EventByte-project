package fr.soft_labs.eventbyte.booking.infrastructure.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketEntity;
import fr.soft_labs.eventbyte.event.infrastructure.persistence.TicketJpaRepository;
import fr.soft_labs.eventbyte.shared.infrastructure.redis.StockKeys;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Test d'intégration sur un vrai Redis (localhost:6379, celui du docker-compose).
 * Ignoré si Redis n'est pas joignable : docker compose up -d redis pour l'exécuter.
 */
class RedisStockReserverTest {

    private LettuceConnectionFactory factory;
    private StringRedisTemplate redis;
    private RedisStockReserver reserver;
    private String ticketA;
    private String ticketB;

    @BeforeEach
    void setUp() {
        factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        try {
            redis.getConnectionFactory().getConnection().ping();
        } catch (Exception e) {
            assumeTrue(false, "Redis indisponible sur localhost:6379");
        }
        ticketA = "test-" + UUID.randomUUID();
        ticketB = "test-" + UUID.randomUUID();
        TicketJpaRepository tickets = mock(TicketJpaRepository.class);
        when(tickets.findAllById(anyIterable())).thenReturn(List.of(
                new TicketEntity(ticketA, "evt", "A", 5, 1000),
                new TicketEntity(ticketB, "evt", "B", 1, 2000)));
        reserver = new RedisStockReserver(redis, tickets);
    }

    @AfterEach
    void tearDown() {
        if (redis != null && ticketA != null) {
            redis.delete(List.of(StockKeys.reserved(ticketA), StockKeys.reserved(ticketB)));
        }
        if (factory != null) {
            factory.destroy();
        }
    }

    private static Booking booking(BookingLine... lines) {
        return Booking.request(UUID.randomUUID().toString(), "user", "evt", Instant.now(),
                java.time.Duration.ofMinutes(10), List.of(lines));
    }

    private int reserved(String ticketId) {
        String value = redis.opsForValue().get(StockKeys.reserved(ticketId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    @Test
    void reserves_while_the_stock_suffices_then_refuses() {
        assertThat(reserver.tryReserve(booking(new BookingLine(ticketA, "A", 3, 1000)))).isTrue();
        assertThat(reserver.tryReserve(booking(new BookingLine(ticketA, "A", 2, 1000)))).isTrue();
        assertThat(reserver.tryReserve(booking(new BookingLine(ticketA, "A", 1, 1000)))).isFalse();

        assertThat(reserved(ticketA)).isEqualTo(5);
    }

    @Test
    void is_all_or_nothing_across_lines() {
        Booking tooBig = booking(
                new BookingLine(ticketA, "A", 2, 1000),
                new BookingLine(ticketB, "B", 2, 2000)); // B n'a qu'1 place

        assertThat(reserver.tryReserve(tooBig)).isFalse();

        assertThat(reserved(ticketA)).isZero();
        assertThat(reserved(ticketB)).isZero();
    }

    @Test
    void release_gives_the_places_back_and_never_goes_below_zero() {
        Booking booking = booking(new BookingLine(ticketA, "A", 4, 1000));
        reserver.tryReserve(booking);

        reserver.release(booking);
        assertThat(reserved(ticketA)).isZero();
        reserver.release(booking);
        assertThat(reserved(ticketA)).isZero();
        assertThat(reserver.tryReserve(booking(new BookingLine(ticketA, "A", 5, 1000)))).isTrue();
    }

    @Test
    void an_unknown_ticket_is_refused() {
        assertThat(reserver.tryReserve(booking(new BookingLine("inconnu", "X", 1, 100)))).isFalse();
    }

    @Test
    void only_one_of_many_simultaneous_bookings_gets_the_last_ticket() throws Exception {
        int attempts = 50;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                if (reserver.tryReserve(booking(new BookingLine(ticketB, "B", 1, 2000)))) {
                    successes.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        pool.shutdown();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(reserved(ticketB)).isEqualTo(1);
    }
}
