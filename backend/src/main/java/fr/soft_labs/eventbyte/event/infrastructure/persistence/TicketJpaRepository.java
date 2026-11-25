package fr.soft_labs.eventbyte.event.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, String> {

    List<TicketEntity> findByEventId(String eventId);

    void deleteByEventId(String eventId);
}