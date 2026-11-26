package fr.soft_labs.eventbyte.booking.infrastructure.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.soft_labs.eventbyte.booking.application.ConfirmBookingPaymentService;
import fr.soft_labs.eventbyte.booking.application.ConfirmBookingPaymentService.Outcome;
import fr.soft_labs.eventbyte.shared.infrastructure.security.SecurityConfig;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StripeWebhookController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "eventbyte.stripe.webhook-secret=" + StripeWebhookControllerTest.SECRET)
class StripeWebhookControllerTest {

    static final String SECRET = "whsec_test_secret";

    @TestConfiguration
    @EnableConfigurationProperties(StripeProperties.class)
    static class Config {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfirmBookingPaymentService confirmBookingPaymentService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static String event(String type, String objectId) {
        return """
                { "id": "evt_1", "object": "event", "type": "%s", "api_version": "2020-08-27",
                  "data": { "object": { "id": "%s", "object": "payment_intent", "status": "succeeded" } } }
                """.formatted(type, objectId);
    }

    /** En-tête "Stripe-Signature" tel que Stripe le calcule : HMAC-SHA256 de "timestamp.payload". */
    private static String sign(String payload, String secret) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(
                mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + signature;
    }

    @Test
    void a_signed_payment_succeeded_event_confirms_the_booking_without_any_jwt() throws Exception {
        when(confirmBookingPaymentService.paymentSucceeded("pi_123")).thenReturn(Outcome.CONFIRMED);
        String payload = event("payment_intent.succeeded", "pi_123");

        mockMvc.perform(post("/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(payload, SECRET))
                        .content(payload))
                .andExpect(status().isOk());

        verify(confirmBookingPaymentService).paymentSucceeded("pi_123");
    }

    @Test
    void a_forged_signature_is_refused_and_nothing_is_confirmed() throws Exception {
        String payload = event("payment_intent.succeeded", "pi_123");

        mockMvc.perform(post("/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(payload, "whsec_pirate"))
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(confirmBookingPaymentService);
    }

    @Test
    void a_tampered_payload_is_refused() throws Exception {
        String signed = event("payment_intent.succeeded", "pi_123");
        String tampered = event("payment_intent.succeeded", "pi_autre");

        mockMvc.perform(post("/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(signed, SECRET))
                        .content(tampered))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(confirmBookingPaymentService);
    }

    @Test
    void a_missing_signature_is_refused() throws Exception {
        mockMvc.perform(post("/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(event("payment_intent.succeeded", "pi_123")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(confirmBookingPaymentService);
    }

    @Test
    void other_events_are_acknowledged_and_ignored() throws Exception {
        String payload = event("payment_intent.payment_failed", "pi_123");

        mockMvc.perform(post("/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(payload, SECRET))
                        .content(payload))
                .andExpect(status().isOk());

        verifyNoInteractions(confirmBookingPaymentService);
    }

    @Test
    void a_processing_error_returns_500_so_that_stripe_retries() throws Exception {
        when(confirmBookingPaymentService.paymentSucceeded(any())).thenThrow(new IllegalStateException("base indisponible"));
        String payload = event("payment_intent.succeeded", "pi_123");

        mockMvc.perform(post("/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(payload, SECRET))
                        .content(payload))
                .andExpect(status().is5xxServerError());
    }
}
