package fr.soft_labs.eventbyte.booking.application;

import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * US-15 : un utilisateur connecté consulte une de ses réservations.
 *
 * Vide si elle n'existe pas OU si elle appartient à quelqu'un d'autre : dans les deux cas
 * l'API répond 404, pour ne pas révéler l'existence des réservations des autres.
 */
@Service
public class GetBookingService {

    private final BookingRepository bookingRepository;

    public GetBookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Optional<Booking> execute(String userId, String bookingId) {
        return bookingRepository.findById(bookingId)
                .filter(booking -> booking.userId().equals(userId));
    }
}
