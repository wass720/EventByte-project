package fr.soft_labs.eventbyte.booking.infrastructure.web;

import fr.soft_labs.eventbyte.booking.application.port.BookingNotifier;
import fr.soft_labs.eventbyte.booking.domain.Booking;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * US-18 : flux SSE des réservations (GET /bookings/stream).
 *
 * Chaque connexion est rangée sous l'utilisateur du JWT ("sub"). Un changement de statut
 * n'est envoyé qu'aux connexions du propriétaire de la réservation (booking.userId) :
 * on ne reçoit jamais les réservations des autres.
 *
 * - Événement "booking_updated", données = même JSON que GET /bookings/:id.
 * - Un commentaire SSE est envoyé régulièrement : il garde la connexion ouverte à travers les
 *   proxys et détecte les clients partis (l'envoi échoue, la connexion est retirée).
 * - Une connexion dure "stream-timeout" puis se ferme : le navigateur se reconnecte seul.
 *
 * Limite : les connexions sont en mémoire. Avec plusieurs instances du backend, il faudrait
 * diffuser les changements entre instances (Redis pub/sub par exemple).
 */
@Component
public class SseBookingNotifier implements BookingNotifier {

    static final String EVENT_NAME = "booking_updated";
    private static final Logger log = LoggerFactory.getLogger(SseBookingNotifier.class);

    private final Map<String, Set<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final Duration streamTimeout;

    public SseBookingNotifier(@Value("${eventbyte.booking.stream-timeout:30m}") Duration streamTimeout) {
        this.streamTimeout = streamTimeout;
    }

    /** Ouvre un flux pour cet utilisateur. */
    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(streamTimeout.toMillis());
        Set<SseEmitter> emitters = emittersByUser.computeIfAbsent(userId, id -> new CopyOnWriteArraySet<>());
        emitters.add(emitter);
        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        // Envoie tout de suite les en-têtes : le client sait que le flux est ouvert.
        send(userId, emitter, SseEmitter.event().comment("connected"));
        return emitter;
    }

    @Override
    public void bookingChanged(Booking booking) {
        try {
            Set<SseEmitter> emitters = emittersByUser.get(booking.userId());
            if (emitters == null) {
                return;
            }
            BookingResponse data = BookingResponse.from(booking);
            for (SseEmitter emitter : emitters) {
                send(booking.userId(), emitter, SseEmitter.event()
                        .name(EVENT_NAME)
                        .data(data, MediaType.APPLICATION_JSON));
            }
        } catch (RuntimeException e) {
            // Une notification perdue ne doit jamais faire échouer un changement de statut.
            log.warn("Notification SSE impossible pour la réservation {}", booking.id(), e);
        }
    }

    @Scheduled(fixedDelayString = "${eventbyte.booking.stream-heartbeat:25s}")
    public void heartbeat() {
        emittersByUser.forEach((userId, emitters) ->
                emitters.forEach(emitter -> send(userId, emitter, SseEmitter.event().comment("ping"))));
    }

    /** Nombre de flux ouverts pour cet utilisateur (tests, supervision). */
    int openStreams(String userId) {
        Set<SseEmitter> emitters = emittersByUser.get(userId);
        return emitters == null ? 0 : emitters.size();
    }

    private void send(String userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            // Client parti ou flux déjà terminé.
            remove(userId, emitter);
        }
    }

    private void remove(String userId, SseEmitter emitter) {
        emittersByUser.computeIfPresent(userId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
