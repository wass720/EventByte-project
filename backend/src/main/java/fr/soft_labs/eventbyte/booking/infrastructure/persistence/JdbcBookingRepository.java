package fr.soft_labs.eventbyte.booking.infrastructure.persistence;

import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Adapter Postgres du port BookingRepository. */
@Repository
public class JdbcBookingRepository implements BookingRepository {

    private final JdbcClient jdbcClient;

    public JdbcBookingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(Booking booking) {
        jdbcClient.sql("""
                        INSERT INTO bookings (id, user_id, event_id, booked_at, expires_at, status, total, fees)
                        VALUES (:id, :userId, :eventId, :bookedAt, :expiresAt, :status, :total, :fees)
                        ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status
                        """)
                .param("id", booking.id())
                .param("userId", booking.userId())
                .param("eventId", booking.eventId())
                .param("bookedAt", Timestamp.from(booking.bookedAt()))
                .param("expiresAt", Timestamp.from(booking.expiresAt()))
                .param("status", booking.status().value())
                .param("total", booking.total())
                .param("fees", booking.fees())
                .update();

        // Les lignes ne changent jamais après la création : ON CONFLICT DO NOTHING.
        for (BookingLine line : booking.lines()) {
            jdbcClient.sql("""
                            INSERT INTO booking_lines (booking_id, ticket_id, name, quantity, unit_price)
                            VALUES (:bookingId, :ticketId, :name, :quantity, :unitPrice)
                            ON CONFLICT (booking_id, ticket_id) DO NOTHING
                            """)
                    .param("bookingId", booking.id())
                    .param("ticketId", line.ticketId())
                    .param("name", line.name())
                    .param("quantity", line.quantity())
                    .param("unitPrice", line.unitPrice())
                    .update();
        }
    }

    @Override
    public List<Booking> findDueForExpiration(Instant now, int limit) {
        return jdbcClient.sql("""
                        SELECT id, user_id, event_id, booked_at, expires_at, status, total, fees
                        FROM bookings
                        WHERE status IN ('reserved', 'awaiting-payment') AND expires_at <= :now
                        ORDER BY expires_at
                        LIMIT :limit
                        """)
                .param("now", Timestamp.from(now))
                .param("limit", limit)
                .query((rs, rowNum) -> new Booking(
                        rs.getString("id"),
                        rs.getString("user_id"),
                        rs.getString("event_id"),
                        rs.getObject("booked_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
                        linesOf(rs.getString("id")),
                        BookingStatus.fromValue(rs.getString("status")),
                        rs.getInt("total"),
                        rs.getInt("fees")))
                .list();
    }

    @Override
    public boolean markExpired(String bookingId) {
        // La condition sur le statut rend le changement atomique : un seul appelant gagne.
        return jdbcClient.sql("""
                        UPDATE bookings SET status = 'expired'
                        WHERE id = :id AND status IN ('reserved', 'awaiting-payment')
                        """)
                .param("id", bookingId)
                .update() == 1;
    }

    private List<BookingLine> linesOf(String bookingId) {
        return jdbcClient.sql("""
                        SELECT ticket_id, name, quantity, unit_price
                        FROM booking_lines WHERE booking_id = :id ORDER BY ticket_id
                        """)
                .param("id", bookingId)
                .query((rs, rowNum) -> new BookingLine(
                        rs.getString("ticket_id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getInt("unit_price")))
                .list();
    }
}
