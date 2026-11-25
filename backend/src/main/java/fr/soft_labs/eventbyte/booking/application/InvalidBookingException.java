package fr.soft_labs.eventbyte.booking.application;

/** La demande de réservation est mal formée (HTTP 400). */
public class InvalidBookingException extends RuntimeException {

    public InvalidBookingException(String message) {
        super(message);
    }
}
