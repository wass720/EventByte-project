package fr.soft_labs.eventbyte.booking.infrastructure.persistence;

import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import fr.soft_labs.eventbyte.booking.domain.BookingPayment;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Adapter Postgres du port BookingRepository. */
@Repository
public class JdbcBookingRepository implements BookingRepository {

    private static final String SELECT_BOOKINGS = """
            SELECT id, user_id, event_id, booked_at, expires_at, status, total, fees,
                   payment_intent_id, client_secret
            FROM bookings""";

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
    public boolean update(Booking booking, BookingStatus expectedStatus) {
        // La condition sur le statut rend le changement atomique : un seul appelant gagne.
        BookingPayment payment = booking.payment();
        return jdbcClient.sql("""
                        UPDATE bookings
                        SET status = :status, payment_intent_id = :paymentIntentId, client_secret = :clientSecret
                        WHERE id = :id AND status = :expectedStatus
                        """)
                .param("id", booking.id())
                .param("status", booking.status().value())
                .param("paymentIntentId", payment == null ? null : payment.id(), Types.VARCHAR)
                .param("clientSecret", payment == null ? null : payment.clientSecret(), Types.VARCHAR)
                .param("expectedStatus", expectedStatus.value())
                .update() == 1;
    }

    @Override
    public Optional<Booking> findById(String bookingId) {
        return toBookings(jdbcClient.sql(SELECT_BOOKINGS + " WHERE id = :id")
                .param("id", bookingId)
                .query(JdbcBookingRepository::mapRow)
                .list()).stream().findFirst();
    }

    @Override
    public Optional<Booking> findByPaymentId(String paymentId) {
        return toBookings(jdbcClient.sql(SELECT_BOOKINGS + " WHERE payment_intent_id = :paymentId")
                .param("paymentId", paymentId)
                .query(JdbcBookingRepository::mapRow)
                .list()).stream().findFirst();
    }

    @Override
    public List<Booking> findByUserId(String userId) {
        return toBookings(jdbcClient.sql(SELECT_BOOKINGS + " WHERE user_id = :userId ORDER BY booked_at DESC")
                .param("userId", userId)
                .query(JdbcBookingRepository::mapRow)
                .list());
    }

    @Override
    public List<Booking> findDueForExpiration(Instant now, int limit) {
        return toBookings(jdbcClient.sql(SELECT_BOOKINGS + """
                         WHERE status IN ('reserved', 'awaiting-payment') AND expires_at <= :now
                         ORDER BY expires_at
                         LIMIT :limit
                        """)
                .param("now", Timestamp.from(now))
                .param("limit", limit)
                .query(JdbcBookingRepository::mapRow)
                .list());
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

    /** Une ligne de la table bookings, sans ses lignes de tickets. */
    private record Row(String id, String userId, String eventId, Instant bookedAt, Instant expiresAt,
                       BookingStatus status, int total, int fees, BookingPayment payment) {
    }

    private static Row mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        String paymentIntentId = rs.getString("payment_intent_id");
        String clientSecret = rs.getString("client_secret");
        return new Row(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("event_id"),
                rs.getObject("booked_at", OffsetDateTime.class).toInstant(),
                rs.getObject("expires_at", OffsetDateTime.class).toInstant(),
                BookingStatus.fromValue(rs.getString("status")),
                rs.getInt("total"),
                rs.getInt("fees"),
                paymentIntentId == null || clientSecret == null
                        ? null
                        : new BookingPayment(paymentIntentId, clientSecret));
    }

    /** Charge les lignes de tous les bookings en UNE requête (pas de N+1), en gardant l'ordre. */
    private List<Booking> toBookings(List<Row> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<String, List<BookingLine>> linesByBooking = new HashMap<>();
        jdbcClient.sql("""
                        SELECT booking_id, ticket_id, name, quantity, unit_price
                        FROM booking_lines WHERE booking_id IN (:ids) ORDER BY ticket_id
                        """)
                .param("ids", rows.stream().map(Row::id).toList())
                .query((rs, rowNum) -> {
                    linesByBooking.computeIfAbsent(rs.getString("booking_id"), k -> new ArrayList<>())
                            .add(new BookingLine(rs.getString("ticket_id"), rs.getString("name"),
                                    rs.getInt("quantity"), rs.getInt("unit_price")));
                    return null;
                })
                .list();
        return rows.stream()
                .map(r -> new Booking(r.id(), r.userId(), r.eventId(), r.bookedAt(), r.expiresAt(),
                        linesByBooking.get(r.id()), r.status(), r.total(), r.fees(), r.payment()))
                .toList();
    }
}
