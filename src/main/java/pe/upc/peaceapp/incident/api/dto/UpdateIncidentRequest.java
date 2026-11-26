package pe.upc.peaceapp.incident.api.dto;

import jakarta.validation.constraints.Size;
import pe.upc.peaceapp.incident.domain.model.IncidentType;

public record UpdateIncidentRequest(
        IncidentType type,
        @Size(max = 50) String title,
        @Size(max = 1000) String description,
        @Size(max = 100) String address) {
}
