package pe.upc.peaceapp.incident.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** US-09: bitacora de auditoria de modificaciones y eliminaciones de reportes. */
@Entity
@Table(name = "incident_audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class IncidentAuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private UUID performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Column(nullable = false)
    private Instant performedAt = Instant.now();

    public IncidentAuditLog(UUID incidentId, UUID performedBy, Action action) {
        this.incidentId = incidentId;
        this.performedBy = performedBy;
        this.action = action;
    }

    public enum Action { CREATED, UPDATED, DELETED }
}
