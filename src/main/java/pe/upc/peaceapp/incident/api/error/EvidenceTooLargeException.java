package pe.upc.peaceapp.incident.api.error;

/** US-08: archivo de evidencia que excede el limite permitido. */
public class EvidenceTooLargeException extends RuntimeException {
    public EvidenceTooLargeException(long limitBytes) {
        super("El archivo de evidencia excede el limite permitido de " + limitBytes + " bytes");
    }
}
