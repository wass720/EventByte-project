package fr.soft_labs.eventbyte.booking.infrastructure.persistence;

import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import java.sql.Timestamp;
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
                        INSERT INTO bookings (id, user_id, event_id, booked_at, status, total, fees)
                        VALUES (:id, :userId, :eventId, :bookedAt, :status, :total, :fees)
                        ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status
                        """)
                .param("id", booking.id())
                .param("userId", booking.userId())
                .param("eventId", booking.eventId())
                .param("bookedAt", Timestamp.from(booking.bookedAt()))
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
}
