package pe.upc.peaceapp.incident.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import pe.upc.peaceapp.incident.domain.model.IncidentType;

import java.util.List;

/** US-04, US-06, US-07, US-08: registro rapido de un incidente con evidencia opcional. */
public record CreateIncidentRequest(
        @NotNull IncidentType type,
        String description,
        @NotNull Double latitude,
        @NotNull Double longitude,
        boolean anonymous,
        @Valid List<EvidenceRequest> evidences
) {
}
