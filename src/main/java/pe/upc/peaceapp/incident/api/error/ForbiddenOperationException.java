package pe.upc.peaceapp.incident.api.error;

/** Un usuario intenta editar, eliminar o consultar un reporte que no le pertenece. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
