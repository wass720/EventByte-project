package fr.soft_labs.eventbyte.booking.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * eventbyte.booking.payment-window : temps laissé pour payer avant expiration (US-14).
 * Choix du groupe : 10 minutes par défaut.
 *
 * eventbyte.booking.fee-per-ticket : frais de service par ticket, en centimes (US-16).
 * 30 par défaut, comme le panier du frontend (0,30 € par ticket).
 *
 * eventbyte.booking.minimum-total : plus petit total payable par carte, en centimes (US-16).
 * 50 par défaut : Stripe refuse tout paiement en euros sous 0,50 €.
 */
@ConfigurationProperties(prefix = "eventbyte.booking")
public record BookingProperties(@DefaultValue("10m") Duration paymentWindow,
                                @DefaultValue("30") int feePerTicket,
                                @DefaultValue("50") int minimumTotal) {
}
