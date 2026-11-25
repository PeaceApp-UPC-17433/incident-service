package pe.upc.peaceapp.incident.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String STATUS_QUEUE = "incident.status-changes";
    public static final String DLQ = "incident.status-changes.dlq";

    @Bean
    TopicExchange eventsExchange(@Value("${incident.messaging.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange(@Value("${incident.messaging.exchange}") String name) {
        return new DirectExchange(name + ".dlx", true, false);
    }

    @Bean
    Declarables statusChangeTopology(TopicExchange eventsExchange, DirectExchange deadLetterExchange,
                                     @Value("${incident.messaging.exchange}") String exchange) {
        var queue = QueueBuilder.durable(STATUS_QUEUE)
                .withArgument("x-dead-letter-exchange", exchange + ".dlx")
                .withArgument("x-dead-letter-routing-key", DLQ).build();
        var dlq = QueueBuilder.durable(DLQ).build();
        var declarables = new java.util.ArrayList<Declarable>(java.util.List.of(queue, dlq,
                BindingBuilder.bind(dlq).to(deadLetterExchange).with(DLQ)));
        for (var key : StatusEventsListener.ROUTING_KEYS) {
            declarables.add(BindingBuilder.bind(queue).to(eventsExchange).with(key));
        }
        return new Declarables(declarables);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

