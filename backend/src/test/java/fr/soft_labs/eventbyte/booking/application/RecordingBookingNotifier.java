package fr.soft_labs.eventbyte.booking.application;

import fr.soft_labs.eventbyte.booking.application.port.BookingNotifier;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingStatus;
import java.util.ArrayList;
import java.util.List;

/** Enregistre les notifications envoyées, pour les vérifier dans les tests. */
class RecordingBookingNotifier implements BookingNotifier {

    final List<Booking> notified = new ArrayList<>();

    @Override
    public void bookingChanged(Booking booking) {
        notified.add(booking);
    }

    List<BookingStatus> statuses() {
        return notified.stream().map(Booking::status).toList();
    }
}
