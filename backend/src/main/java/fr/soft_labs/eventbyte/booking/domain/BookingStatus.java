package fr.soft_labs.eventbyte.booking.domain;

/**
 * Cycle de vie d'une réservation. La valeur "api" est celle du contrat du README.
 * Domaine pur : aucune dépendance à Spring ni à JPA.
 */
public enum BookingStatus {
    REQUESTED("requested"),
    RESERVED("reserved"),
    REJECTED("rejected"),
    AWAITING_PAYMENT("awaiting-payment"),
    EXPIRED("expired"),
    CONFIRMED("confirmed");

    private final String value;

    BookingStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static BookingStatus fromValue(String value) {
        for (BookingStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Statut de réservation inconnu : " + value);
    }
}
