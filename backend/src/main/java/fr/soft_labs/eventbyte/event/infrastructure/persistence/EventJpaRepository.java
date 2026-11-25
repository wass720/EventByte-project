package fr.soft_labs.eventbyte.event.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventJpaRepository extends JpaRepository<EventEntity, String> {
}