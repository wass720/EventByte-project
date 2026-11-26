package fr.soft_labs.eventbyte.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Strapi saisit les prix en euros ; le backend les stocke en centimes. */
class EventIngestionPriceTest {

    @Test
    void euros_from_strapi_become_cents() {
        assertEquals(2500, EventIngestionService.toCents(new BigDecimal("25")));
        assertEquals(2550, EventIngestionService.toCents(new BigDecimal("25.5")));
        assertEquals(1399, EventIngestionService.toCents(new BigDecimal("13.99")));
        assertEquals(0, EventIngestionService.toCents(BigDecimal.ZERO));
    }

    @Test
    void sub_cent_values_are_rounded_to_the_nearest_cent() {
        assertEquals(1000, EventIngestionService.toCents(new BigDecimal("9.995")));
        assertEquals(999, EventIngestionService.toCents(new BigDecimal("9.994")));
    }

    @Test
    void a_missing_price_stays_missing() {
        assertNull(EventIngestionService.toCents(null));
    }
}
