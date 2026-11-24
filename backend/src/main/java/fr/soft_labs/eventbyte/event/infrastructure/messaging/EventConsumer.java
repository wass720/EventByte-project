package fr.soft_labs.eventbyte.event.infrastructure.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onMessage(Message message,
                          @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        String body = new String(message.getBody());
        System.out.println("Message reçu [" + routingKey + "] : " + body);
    }
}