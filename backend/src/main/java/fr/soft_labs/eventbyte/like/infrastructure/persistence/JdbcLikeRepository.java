package fr.soft_labs.eventbyte.like.infrastructure.persistence;

import fr.soft_labs.eventbyte.like.application.port.LikeRepository;
import fr.soft_labs.eventbyte.like.domain.Like;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Adapter Postgres du port LikeRepository.
 *
 * JdbcClient plutôt que JPA : "ON CONFLICT DO NOTHING" rend l'insertion
 * idempotente de façon atomique (deux likes simultanés du même utilisateur
 * ne provoquent ni doublon ni erreur de clé primaire).
 */
@Repository
public class JdbcLikeRepository implements LikeRepository {

    private final JdbcClient jdbcClient;

    public JdbcLikeRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(Like like) {
        jdbcClient.sql("""
                        INSERT INTO likes (user_id, event_id)
                        VALUES (:userId, :eventId)
                        ON CONFLICT (user_id, event_id) DO NOTHING
                        """)
                .param("userId", like.userId())
                .param("eventId", like.eventId())
                .update();
    }

    @Override
    public void delete(Like like) {
        jdbcClient.sql("DELETE FROM likes WHERE user_id = :userId AND event_id = :eventId")
                .param("userId", like.userId())
                .param("eventId", like.eventId())
                .update();
    }

    @Override
    public int countByEventId(String eventId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM likes WHERE event_id = :eventId")
                .param("eventId", eventId)
                .query(Integer.class)
                .single();
    }

    @Override
    public List<String> findEventIdsByUserId(String userId) {
        return jdbcClient.sql("""
                        SELECT event_id FROM likes
                        WHERE user_id = :userId
                        ORDER BY liked_at DESC
                        """)
                .param("userId", userId)
                .query(String.class)
                .list();
    }
}
