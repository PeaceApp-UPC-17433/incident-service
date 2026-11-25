package pe.upc.peaceapp.incident.domain.repository;

import pe.upc.peaceapp.incident.domain.model.IncidentStatus;
import pe.upc.peaceapp.incident.domain.model.IncidentType;
import pe.upc.peaceapp.incident.domain.model.Severity;

import java.time.Instant;
import java.util.UUID;

public record IncidentFilter(IncidentType type, Severity severity, IncidentStatus status, String text, Instant from,
                             Instant to, UUID authorId, Bounds bounds, boolean excludeDiscarded) {

    public record Bounds(double minLat, double maxLat, double minLng, double maxLng) {
    }
}
