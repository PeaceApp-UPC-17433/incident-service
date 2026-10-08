package pe.upc.peaceapp.incident.api.error;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(UUID id) {
        super("Incidente no encontrado: " + id);
    }
}
