package pe.upc.peaceapp.incident.api.error;

/** US-09: la edicion/eliminacion solo esta permitida dentro de la ventana de tiempo. */
public class EditWindowClosedException extends RuntimeException {
    public EditWindowClosedException() {
        super("El reporte ya no se puede editar ni eliminar: la ventana de tiempo expiro");
    }
}
