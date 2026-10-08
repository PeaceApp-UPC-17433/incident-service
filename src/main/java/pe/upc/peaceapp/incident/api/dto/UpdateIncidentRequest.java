package pe.upc.peaceapp.incident.api.dto;

import pe.upc.peaceapp.incident.domain.model.IncidentType;

/** US-09: edicion dentro de la ventana de tiempo. */
public record UpdateIncidentRequest(IncidentType type, String description) {
}
