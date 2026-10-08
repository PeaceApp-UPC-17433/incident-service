package pe.upc.peaceapp.incident.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** BC-02 Incident Reporting: ciclo de vida del reporte (US-04, US-06 a US-10, US-21). */
@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
public class Incident {

    @Id
    @GeneratedValue
    private UUID id;

    /** Siempre se conserva internamente, incluso si anonymous=true (US-04), para auditoria. */
    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    private boolean anonymous = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.PENDIENTE_VALIDACION;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evidence> evidences = new ArrayList<>();

    public Incident(UUID authorId, boolean anonymous, IncidentType type, String description,
                     double latitude, double longitude) {
        this.authorId = authorId;
        this.anonymous = anonymous;
        this.type = type;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean isWithinEditWindow(Instant now, long editWindowMinutes) {
        return now.isBefore(createdAt.plusSeconds(editWindowMinutes * 60));
    }
}
