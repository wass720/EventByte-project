package fr.soft_labs.eventbyte.booking.infrastructure.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fr.soft_labs.eventbyte.booking.application.PaymentUnavailableException;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import fr.soft_labs.eventbyte.booking.domain.BookingPayment;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Teste l'adapter contre un faux serveur Stripe local : vérifie la requête HTTP réellement
 * envoyée par le SDK (montant en centimes, devise, idempotence...) sans appeler Stripe.
 */
class StripePaymentGatewayTest {

    private HttpServer server;
    private final List<Recorded> requests = new ArrayList<>();
    private int status;
    private String responseBody;
    /** Réponses par route ("POST /v1/..."), prioritaires sur status/responseBody. */
    private final Map<String, String[]> routes = new HashMap<>();

    private record Recorded(String method, String path, String authorization, String idempotencyKey, String body) {
    }

    @BeforeEach
    void startFakeStripe() throws IOException {
        status = 200;
        responseBody = """
                { "id": "pi_123", "object": "payment_intent", "amount": 5060, "currency": "eur",
                  "client_secret": "pi_123_secret_abc", "status": "requires_payment_method" }
                """;
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void stopFakeStripe() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = URLDecoder.decode(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        requests.add(new Recorded(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("Idempotency-Key"), body));
        String[] routed = routes.get(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
        int code = routed == null ? status : Integer.parseInt(routed[0]);
        byte[] bytes = (routed == null ? responseBody : routed[1]).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private StripePaymentGateway gateway(String secretKey) {
        return new StripePaymentGateway(new StripeProperties(secretKey, "eur",
                "http://localhost:" + server.getAddress().getPort(), Duration.ofSeconds(5)));
    }

    private static Booking reserved() {
        return Booking.request("b-1", "user-1", "evt-1", Instant.parse("2026-01-01T10:00:00Z"),
                Duration.ofMinutes(10), 30, List.of(new BookingLine("t-1", "Early Bird", 2, 2500))).reserve();
    }

    @Test
    void creates_a_payment_intent_for_the_total_in_cents_and_returns_its_client_secret() {
        BookingPayment payment = gateway("sk_test_x").createPayment(reserved());

        assertEquals(new BookingPayment("pi_123", "pi_123_secret_abc"), payment);

        Recorded request = requests.get(0);
        assertEquals("POST", request.method());
        assertEquals("/v1/payment_intents", request.path());
        assertEquals("Bearer sk_test_x", request.authorization());
        assertTrue(request.body().contains("amount=5060"), request.body()); // 2 x 2500 + 2 x 30
        assertTrue(request.body().contains("currency=eur"), request.body());
        assertTrue(request.body().contains("automatic_payment_methods[enabled]=true"), request.body());
        assertTrue(request.body().contains("automatic_payment_methods[allow_redirects]=never"), request.body());
        assertTrue(request.body().contains("metadata[booking_id]=b-1"), request.body());
        assertTrue(request.body().contains("metadata[user_id]=user-1"), request.body());
        assertTrue(request.body().contains("metadata[event_id]=evt-1"), request.body());
    }

    @Test
    void the_same_booking_always_uses_the_same_idempotency_key() {
        StripePaymentGateway gateway = gateway("sk_test_x");

        gateway.createPayment(reserved());
        gateway.createPayment(reserved());

        assertEquals("booking-b-1-payment", requests.get(0).idempotencyKey());
        assertEquals(requests.get(0).idempotencyKey(), requests.get(1).idempotencyKey());
    }

    @Test
    void a_stripe_error_becomes_a_payment_unavailable_exception() {
        status = 400;
        responseBody = """
                { "error": { "type": "invalid_request_error", "code": "amount_too_small",
                             "message": "Amount must be at least €0.50 eur" } }
                """;

        PaymentUnavailableException e = assertThrows(PaymentUnavailableException.class,
                () -> gateway("sk_test_x").createPayment(reserved()));
        assertTrue(e.getMessage().contains("Amount must be at least"), e.getMessage());
    }

    @Test
    void cancels_a_payment_intent() {
        responseBody = """
                { "id": "pi_123", "object": "payment_intent", "status": "canceled" }
                """;

        gateway("sk_test_x").cancelPayment("pi_123");

        assertEquals("POST", requests.get(0).method());
        assertEquals("/v1/payment_intents/pi_123/cancel", requests.get(0).path());
    }

    private static final String CANNOT_CANCEL = """
            { "error": { "type": "invalid_request_error", "code": "payment_intent_unexpected_state",
                         "message": "This PaymentIntent's status is %s, so it cannot be canceled." } }
            """;

    @Test
    void cancelling_an_already_canceled_payment_intent_is_a_success() {
        routes.put("POST /v1/payment_intents/pi_123/cancel", new String[] {"400", CANNOT_CANCEL.formatted("canceled")});
        routes.put("GET /v1/payment_intents/pi_123", new String[] {"200", """
                { "id": "pi_123", "object": "payment_intent", "status": "canceled" }
                """});

        gateway("sk_test_x").cancelPayment("pi_123");

        assertEquals("/v1/payment_intents/pi_123", requests.get(1).path());
    }

    @Test
    void an_already_paid_payment_intent_cannot_be_cancelled() {
        routes.put("POST /v1/payment_intents/pi_123/cancel", new String[] {"400", CANNOT_CANCEL.formatted("succeeded")});
        routes.put("GET /v1/payment_intents/pi_123", new String[] {"200", """
                { "id": "pi_123", "object": "payment_intent", "status": "succeeded" }
                """});

        assertThrows(PaymentUnavailableException.class, () -> gateway("sk_test_x").cancelPayment("pi_123"));
    }

    @Test
    void without_a_secret_key_nothing_is_sent_and_the_payment_is_unavailable() {
        StripePaymentGateway gateway = gateway("  ");

        assertThrows(PaymentUnavailableException.class, () -> gateway.createPayment(reserved()));
        assertThrows(PaymentUnavailableException.class, () -> gateway.cancelPayment("pi_123"));
        assertTrue(requests.isEmpty());
    }
}
