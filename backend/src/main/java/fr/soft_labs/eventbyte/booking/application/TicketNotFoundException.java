package fr.soft_labs.eventbyte.booking.application;

/** Un ticket demandé n'existe pas (HTTP 404). */
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String ticketId) {
        super("Ticket introuvable : " + ticketId);
    }
}
