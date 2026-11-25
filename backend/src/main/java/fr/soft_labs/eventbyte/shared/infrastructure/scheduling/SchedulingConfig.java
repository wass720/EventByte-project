package fr.soft_labs.eventbyte.shared.infrastructure.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Active les tâches @Scheduled (expiration des réservations, US-14). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
