package pe.upc.peaceapp.incident.api.dto;

import pe.upc.peaceapp.incident.domain.model.Incident;
import pe.upc.peaceapp.incident.domain.model.IncidentStatus;
import pe.upc.peaceapp.incident.domain.model.IncidentType;
import pe.upc.peaceapp.incident.domain.model.Severity;

import java.time.Instant;
import java.util.UUID;

public record IncidentSummaryResponse(
        UUID id,
        UUID authorId,
        IncidentType type,
        Severity severity,
        String title,
        String description,
        String address,
        double latitude,
        double longitude,
        IncidentStatus status,
        Instant occurredAt,
        Instant createdAt
) {
    public static IncidentSummaryResponse from(Incident incident) {
        return new IncidentSummaryResponse(
                incident.getId(),
                incident.isAnonymous() ? null : incident.getAuthorId(),
                incident.getType(),
                incident.getType().severity(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getAddress(),
                incident.getLatitude(),
                incident.getLongitude(),
                incident.getStatus(),
                incident.getOccurredAt(),
                incident.getCreatedAt());
    }
}
