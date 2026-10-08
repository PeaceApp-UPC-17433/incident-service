package pe.upc.peaceapp.incident.api.dto;

import pe.upc.peaceapp.incident.domain.model.Incident;
import pe.upc.peaceapp.incident.domain.model.IncidentStatus;
import pe.upc.peaceapp.incident.domain.model.IncidentType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** US-21: detalle del incidente. El autor se omite cuando anonymous=true (US-04). */
public record IncidentResponse(
        UUID id,
        UUID authorId,
        IncidentType type,
        String description,
        double latitude,
        double longitude,
        IncidentStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<EvidenceResponse> evidences
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.isAnonymous() ? null : incident.getAuthorId(),
                incident.getType(),
                incident.getDescription(),
                incident.getLatitude(),
                incident.getLongitude(),
                incident.getStatus(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getEvidences().stream().map(EvidenceResponse::from).toList());
    }
}
