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

/**
 * US-12 : un utilisateur connecté réserve un ou plusieurs types de tickets.
 *
 * La réservation est créée "requested", puis passe "reserved" si le stock suffit,
 * "rejected" sinon. Le blocage du stock (Redis, US-13) est atomique et n'est pas dans
 * une transaction SQL : si l'enregistrement du statut final échoue, les places bloquées
 * sont rendues pour ne pas les perdre.
 *
 * US-16 : une réservation "reserved" voit tout de suite son paiement préparé
 * (StartBookingPaymentService) : quand le frontend la lit, elle est déjà "awaiting-payment"
 * avec son clientSecret (ou "rejected" si le paiement n'a pas pu être préparé).
 */
@Service
public class CreateBookingService {

    private final BookableTickets bookableTickets;
    private final StockReserver stockReserver;
    private final BookingRepository bookingRepository;
    private final StartBookingPaymentService startBookingPaymentService;
    private final Clock clock;
    private final BookingProperties properties;

    public CreateBookingService(BookableTickets bookableTickets,
                                StockReserver stockReserver,
                                BookingRepository bookingRepository,
                                StartBookingPaymentService startBookingPaymentService,
                                Clock clock,
                                BookingProperties properties) {
        this.bookableTickets = bookableTickets;
        this.stockReserver = stockReserver;
        this.bookingRepository = bookingRepository;
        this.startBookingPaymentService = startBookingPaymentService;
        this.clock = clock;
        this.properties = properties;
    }

    /** @return l'id de la réservation, qu'elle ait été acceptée ou rejetée. */
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
                UUID.randomUUID().toString(), userId, eventIds.get(0), clock.instant(),
                properties.paymentWindow(), properties.feePerTicket(), lines);
        // Refusé avant de bloquer des places : Stripe refuserait ce paiement à coup sûr.
        if (requested.total() < properties.minimumTotal()) {
            throw new InvalidBookingException("Montant inférieur au minimum payable par carte ("
                    + properties.minimumTotal() + " centimes)");
        }
        bookingRepository.save(requested);

        boolean reserved = stockReserver.tryReserve(requested);
        Booking decided = reserved ? requested.reserve() : requested.reject();
        try {
            bookingRepository.save(decided);
        } catch (RuntimeException e) {
            if (reserved) {
                stockReserver.release(requested);
            }
            throw e;
        }
        if (reserved) {
            startBookingPaymentService.execute(decided);
        }
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
