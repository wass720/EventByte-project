package fr.soft_labs.eventbyte.booking.infrastructure.stock;

import fr.soft_labs.eventbyte.booking.application.port.StockReserver;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Implémentation provisoire de StockReserver (US-12) : le stock disponible est
 * "quantité du ticket - places des réservations reserved / awaiting-payment".
 *
 * Les lignes de tickets sont verrouillées (FOR UPDATE) jusqu'à la fin de la transaction
 * de CreateBookingService, donc deux réservations du même ticket sont sérialisées.
 * Les tickets sont verrouillés dans l'ordre de leur id pour éviter les deadlocks.
 *
 * À REMPLACER par l'adapter Redis atomique de US-13 (sinon Spring trouvera deux beans).
 */
@Component
public class DatabaseStockReserver implements StockReserver {

    private final JdbcClient jdbcClient;

    public DatabaseStockReserver(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean tryReserve(Booking booking) {
        return booking.lines().stream()
                .sorted(Comparator.comparing(BookingLine::ticketId))
                .allMatch(this::hasEnoughStock);
    }

    private boolean hasEnoughStock(BookingLine line) {
        Optional<Integer> total = jdbcClient
                .sql("SELECT quantity FROM tickets WHERE id = :id FOR UPDATE")
                .param("id", line.ticketId())
                .query(Integer.class)
                .optional();
        if (total.isEmpty()) {
            return false;
        }
        int blocked = jdbcClient.sql("""
                        SELECT COALESCE(SUM(bl.quantity), 0)
                        FROM booking_lines bl
                        JOIN bookings b ON b.id = bl.booking_id
                        WHERE bl.ticket_id = :id AND b.status IN ('reserved', 'awaiting-payment')
                        """)
                .param("id", line.ticketId())
                .query(Integer.class)
                .single();
        return total.get() - blocked >= line.quantity();
    }
}
