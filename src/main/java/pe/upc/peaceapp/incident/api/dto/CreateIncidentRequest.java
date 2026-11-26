package pe.upc.peaceapp.incident.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.upc.peaceapp.incident.domain.model.IncidentType;

import java.time.Instant;
import java.util.List;

public record CreateIncidentRequest(
        @NotNull IncidentType type,
        @Size(max = 50) String title,
        @Size(max = 1000) String description,
        @Size(max = 100) String address,
        Instant occurredAt,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        boolean anonymous,
        @Valid List<EvidenceRequest> evidences
) {
}
