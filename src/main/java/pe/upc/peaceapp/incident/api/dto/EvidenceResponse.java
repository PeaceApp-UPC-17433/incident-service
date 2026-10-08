package pe.upc.peaceapp.incident.api.dto;

import pe.upc.peaceapp.incident.domain.model.Evidence;

import java.util.UUID;

public record EvidenceResponse(UUID id, Evidence.EvidenceType mediaType, String url, long sizeBytes) {
    public static EvidenceResponse from(Evidence evidence) {
        return new EvidenceResponse(evidence.getId(), evidence.getMediaType(), evidence.getUrl(), evidence.getSizeBytes());
    }
}
