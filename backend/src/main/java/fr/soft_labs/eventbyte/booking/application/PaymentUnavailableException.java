package fr.soft_labs.eventbyte.booking.application;

/** Le prestataire de paiement a refusé ou n'a pas pu traiter la demande. */
public class PaymentUnavailableException extends RuntimeException {

    public PaymentUnavailableException(String message) {
        super(message);
    }

    public PaymentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
