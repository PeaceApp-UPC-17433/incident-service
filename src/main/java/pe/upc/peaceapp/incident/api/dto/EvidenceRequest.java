package pe.upc.peaceapp.incident.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import pe.upc.peaceapp.incident.domain.model.Evidence;

/** US-08: metadata de un adjunto (la subida binaria la resuelve un almacenamiento de objetos externo). */
public record EvidenceRequest(
        @NotNull Evidence.EvidenceType mediaType,
        @NotBlank String url,
        @Positive long sizeBytes
) {
}
