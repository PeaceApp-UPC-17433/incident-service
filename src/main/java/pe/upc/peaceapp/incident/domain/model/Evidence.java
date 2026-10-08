package pe.upc.peaceapp.incident.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** US-08: evidencia multimedia adjunta a un reporte. */
@Entity
@Table(name = "evidences")
@Getter
@Setter
@NoArgsConstructor
public class Evidence {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceType mediaType;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private long sizeBytes;

    public Evidence(Incident incident, EvidenceType mediaType, String url, long sizeBytes) {
        this.incident = incident;
        this.mediaType = mediaType;
        this.url = url;
        this.sizeBytes = sizeBytes;
    }

    public enum EvidenceType { FOTO, VIDEO }
}
