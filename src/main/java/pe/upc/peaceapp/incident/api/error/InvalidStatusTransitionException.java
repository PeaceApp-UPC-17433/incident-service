package pe.upc.peaceapp.incident.api.error;

import pe.upc.peaceapp.incident.domain.model.IncidentStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(IncidentStatus from, IncidentStatus to) {
        super("No se puede pasar de " + from + " a " + to);
    }
}
