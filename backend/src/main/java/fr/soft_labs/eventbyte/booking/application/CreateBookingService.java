package fr.soft_labs.eventbyte.booking.application;

import fr.soft_labs.eventbyte.booking.application.port.BookableTickets;
import fr.soft_labs.eventbyte.booking.application.port.BookableTickets.BookableTicket;
import fr.soft_labs.eventbyte.booking.application.port.BookingRepository;
import fr.soft_labs.eventbyte.booking.application.port.StockReserver;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import fr.soft_labs.eventbyte.booking.domain.BookingLine;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-12 : un utilisateur connecté réserve un ou plusieurs types de tickets.
 *
 * La réservation est créée "requested", puis passe "reserved" si le stock suffit,
 * "rejected" sinon. Le tout est dans une seule transaction : le verrou pris par
 * StockReserver reste tenu jusqu'à l'enregistrement du statut final.
 */
@Service
public class CreateBookingService {

    private final BookableTickets bookableTickets;
    private final StockReserver stockReserver;
    private final BookingRepository bookingRepository;
    private final Clock clock;

    public CreateBookingService(BookableTickets bookableTickets,
                                StockReserver stockReserver,
                                BookingRepository bookingRepository,
                                Clock clock) {
        this.bookableTickets = bookableTickets;
        this.stockReserver = stockReserver;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    /** @return l'id de la réservation, qu'elle ait été acceptée ou rejetée. */
    @Transactional
    public String execute(String userId, List<Item> items) {
        Map<String, Integer> quantities = mergeByTicket(items);

        Map<String, BookableTicket> tickets = bookableTickets.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(BookableTicket::id, ticket -> ticket));
        for (String ticketId : quantities.keySet()) {
            if (!tickets.containsKey(ticketId)) {
                throw new TicketNotFoundException(ticketId);
            }
        }

        List<String> eventIds = tickets.values().stream().map(BookableTicket::eventId).distinct().toList();
        if (eventIds.size() != 1) {
            throw new InvalidBookingException("Une réservation ne peut concerner qu'un seul événement");
        }

        List<BookingLine> lines = quantities.entrySet().stream()
                .map(entry -> {
                    BookableTicket ticket = tickets.get(entry.getKey());
                    return new BookingLine(ticket.id(), ticket.name(), entry.getValue(), ticket.price());
                })
                .toList();

        Booking requested = Booking.request(
                UUID.randomUUID().toString(), userId, eventIds.get(0), clock.instant(), lines);
        bookingRepository.save(requested);

        Booking decided = stockReserver.tryReserve(requested) ? requested.reserve() : requested.reject();
        bookingRepository.save(decided);
        return decided.id();
    }

    /** Regroupe les lignes d'un même ticket (2 + 3 = 5) et refuse les valeurs invalides. */
    private static Map<String, Integer> mergeByTicket(List<Item> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidBookingException("Une réservation contient au moins un ticket");
        }
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (Item item : items) {
            if (item == null || item.ticketId() == null || item.ticketId().isBlank()) {
                throw new InvalidBookingException("Identifiant de ticket manquant");
            }
            if (item.quantity() <= 0) {
                throw new InvalidBookingException("La quantité doit être strictement positive");
            }
            quantities.merge(item.ticketId(), item.quantity(), Integer::sum);
        }
        return quantities;
    }

    public record Item(String ticketId, int quantity) {
    }
}
