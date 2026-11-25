package fr.soft_labs.eventbyte.booking.infrastructure.web;

import java.util.List;

/** Corps de POST /bookings : { items: [{ id: ticketId, quantity }] }. */
public record CreateBookingRequest(List<Item> items) {

    public record Item(String id, int quantity) {
    }
}
