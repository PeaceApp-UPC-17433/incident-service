package pe.upc.peaceapp.incident.domain.model;

import java.util.Arrays;
import java.util.List;

public enum IncidentType {
    ROBO(Severity.HIGH),
    HURTO(Severity.MEDIUM),
    ACCIDENTE_TRANSITO(Severity.MEDIUM),
    AGRESION(Severity.HIGH),
    VANDALISMO(Severity.LOW),
    OTRO(Severity.LOW);

    private final Severity severity;

    IncidentType(Severity severity) {
        this.severity = severity;
    }

    public Severity severity() {
        return severity;
    }

    public static List<IncidentType> withSeverity(Severity severity) {
        return Arrays.stream(values()).filter(t -> t.severity == severity).toList();
    }
}
