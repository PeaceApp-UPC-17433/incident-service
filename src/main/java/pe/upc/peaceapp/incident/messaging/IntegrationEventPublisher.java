package pe.upc.peaceapp.incident.messaging;

public interface IntegrationEventPublisher {
    void publish(String routingKey, Object payload);
}

