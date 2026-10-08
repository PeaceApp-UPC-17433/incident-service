package pe.upc.peaceapp.incident.domain.repository;

import pe.upc.peaceapp.incident.domain.model.IncidentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IncidentAuditLogRepository extends JpaRepository<IncidentAuditLog, UUID> {
}
