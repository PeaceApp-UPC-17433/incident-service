package pe.upc.peaceapp.incident.api.dto;

import pe.upc.peaceapp.incident.domain.model.Incident;
import pe.upc.peaceapp.incident.domain.model.IncidentStatus;
import pe.upc.peaceapp.incident.domain.model.IncidentType;
import pe.upc.peaceapp.incident.domain.model.Severity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentResponse(
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
        Instant createdAt,
        Instant updatedAt,
        List<EvidenceResponse> evidences
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
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
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getEvidences().stream().map(EvidenceResponse::from).toList());
    }

    public IncidentResponse withEvidenceUrls(java.util.function.UnaryOperator<String> urls) {
        return new IncidentResponse(id, authorId, type, severity, title, description, address, latitude, longitude,
                status, occurredAt, createdAt, updatedAt,
                evidences.stream().map(e -> new EvidenceResponse(e.id(), e.mediaType(), urls.apply(e.url()), e.sizeBytes())).toList());
    }
}
