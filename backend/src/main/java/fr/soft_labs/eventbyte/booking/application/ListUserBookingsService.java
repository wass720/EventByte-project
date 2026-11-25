package fr.soft_labs.eventbyte.booking.application;

import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import java.util.List;
import org.springframework.stereotype.Service;

/** US-15 : un utilisateur connecté liste ses réservations (et seulement les siennes). */
@Service
public class ListUserBookingsService {

    private final BookingRepository bookingRepository;

    public ListUserBookingsService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> execute(String userId) {
        return bookingRepository.findByUserId(userId);
    }
}
