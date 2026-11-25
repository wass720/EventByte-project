package fr.soft_labs.eventbyte.event.infrastructure.messaging;

import fr.soft_labs.eventbyte.event.application.EventIngestionService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class EventConsumer {

    private final JsonMapper jsonMapper;
    private final EventIngestionService ingestionService;

    public EventConsumer(JsonMapper jsonMapper, EventIngestionService ingestionService) {
        this.jsonMapper = jsonMapper;
        this.ingestionService = ingestionService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onMessage(Message message,
                          @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        String body = new String(message.getBody());
        EventMessage event = jsonMapper.readValue(body, EventMessage.class);

        switch (routingKey) {
            case "event.created", "event.updated" -> ingestionService.upsert(event);
            case "event.deleted" -> ingestionService.delete(event.id());
            default -> System.out.println("Type de message inconnu : " + routingKey);
        }
        System.out.println("[" + routingKey + "] traité, id=" + event.id());
    }
}