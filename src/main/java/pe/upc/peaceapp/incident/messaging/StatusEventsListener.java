package pe.upc.peaceapp.incident.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pe.upc.peaceapp.incident.application.IncidentService;
import pe.upc.peaceapp.incident.domain.model.IncidentStatus;
import pe.upc.peaceapp.incident.observability.EventContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class StatusEventsListener {
    private static final Logger log = LoggerFactory.getLogger(StatusEventsListener.class);

    static final Map<String, IncidentStatus> STATUS_BY_EVENT = Map.of(
            "community.incident-confirmed", IncidentStatus.VALIDADO,
            "community.incident-discarded", IncidentStatus.DESCARTADO,
            "trust.incident-held", IncidentStatus.EN_MODERACION,
            "trust.incident-approved", IncidentStatus.VALIDADO,
            "trust.incident-rejected", IncidentStatus.DESCARTADO);
    static final List<String> ROUTING_KEYS = List.copyOf(STATUS_BY_EVENT.keySet());

    private final IncidentService incidentService;

    public StatusEventsListener(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    public record StatusEvent(UUID eventId, String correlationId, UUID incidentId) { }

    @RabbitListener(queues = RabbitConfig.STATUS_QUEUE)
    public void on(StatusEvent event, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        EventContext.run(event.correlationId(), event.eventId(), () -> {
            var target = STATUS_BY_EVENT.get(routingKey);
            if (target == null || event.incidentId() == null) {
                log.warn("Evento de estado invalido routingKey={} eventId={}", routingKey, event.eventId());
                throw new AmqpRejectAndDontRequeueException("Evento de estado invalido: " + routingKey);
            }
            boolean applied = incidentService.applyExternalStatus(event.incidentId(), target);
            log.info("{} incidentId={} {}", routingKey, event.incidentId(), applied ? "actualizado a " + target : "sin cambios");
        });
    }
}

