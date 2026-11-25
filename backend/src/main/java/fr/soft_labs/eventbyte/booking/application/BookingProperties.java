package fr.soft_labs.eventbyte.booking.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * eventbyte.booking.payment-window : temps laissé pour payer avant expiration (US-14).
 * Choix du groupe : 10 minutes par défaut.
 */
@ConfigurationProperties(prefix = "eventbyte.booking")
public record BookingProperties(@DefaultValue("10m") Duration paymentWindow) {
}
