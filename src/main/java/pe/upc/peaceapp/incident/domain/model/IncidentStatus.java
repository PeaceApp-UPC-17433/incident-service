package pe.upc.peaceapp.incident.domain.model;

/**
 * US-06: estado inicial "Pendiente de validacion". Los estados Validado/Descartado
 * los asigna validation-service (BC-04) y En moderacion lo asigna trust-service (BC-03);
 * aqui solo se refleja el ultimo estado conocido hasta que esos eventos se consuman.
 */
public enum IncidentStatus {
    PENDIENTE_VALIDACION,
    VALIDADO,
    DESCARTADO,
    EN_MODERACION
}
