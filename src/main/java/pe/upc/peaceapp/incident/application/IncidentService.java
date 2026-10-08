package pe.upc.peaceapp.incident.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.incident.api.dto.CreateIncidentRequest;
import pe.upc.peaceapp.incident.api.dto.EvidenceRequest;
import pe.upc.peaceapp.incident.api.dto.UpdateIncidentRequest;
import pe.upc.peaceapp.incident.api.error.EditWindowClosedException;
import pe.upc.peaceapp.incident.api.error.EvidenceTooLargeException;
import pe.upc.peaceapp.incident.api.error.ForbiddenOperationException;
import pe.upc.peaceapp.incident.api.error.IncidentNotFoundException;
import pe.upc.peaceapp.incident.domain.model.Evidence;
import pe.upc.peaceapp.incident.domain.model.Incident;
import pe.upc.peaceapp.incident.domain.model.IncidentAuditLog;
import pe.upc.peaceapp.incident.domain.repository.IncidentAuditLogRepository;
import pe.upc.peaceapp.incident.domain.repository.IncidentRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** BC-02: ciclo de vida del reporte (US-04, US-06 a US-10, US-21). */
@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentAuditLogRepository auditLogRepository;
    private final long editWindowMinutes;
    private final long maxEvidenceSizeBytes;

    public IncidentService(IncidentRepository incidentRepository,
                            IncidentAuditLogRepository auditLogRepository,
                            @Value("${incident.edit-window-minutes}") long editWindowMinutes,
                            @Value("${incident.max-evidence-size-bytes}") long maxEvidenceSizeBytes) {
        this.incidentRepository = incidentRepository;
        this.auditLogRepository = auditLogRepository;
        this.editWindowMinutes = editWindowMinutes;
        this.maxEvidenceSizeBytes = maxEvidenceSizeBytes;
    }

    @Transactional
    public Incident create(UUID authorId, CreateIncidentRequest request) {
        Incident incident = new Incident(authorId, request.anonymous(), request.type(),
                request.description(), request.latitude(), request.longitude());

        if (request.evidences() != null) {
            for (EvidenceRequest evidenceRequest : request.evidences()) {
                if (evidenceRequest.sizeBytes() > maxEvidenceSizeBytes) {
                    throw new EvidenceTooLargeException(maxEvidenceSizeBytes);
                }
                incident.getEvidences().add(new Evidence(incident, evidenceRequest.mediaType(),
                        evidenceRequest.url(), evidenceRequest.sizeBytes()));
            }
        }

        Incident saved = incidentRepository.save(incident);
        auditLogRepository.save(new IncidentAuditLog(saved.getId(), authorId, IncidentAuditLog.Action.CREATED));
        return saved;
    }

    public Incident getById(UUID incidentId) {
        return incidentRepository.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));
    }

    public List<Incident> getOwnHistory(UUID authorId) {
        return incidentRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
    }

    @Transactional
    public Incident update(UUID incidentId, UUID requesterId, UpdateIncidentRequest request) {
        Incident incident = getOwnedIncidentOrThrow(incidentId, requesterId);
        assertWithinEditWindow(incident);

        if (request.type() != null) {
            incident.setType(request.type());
        }
        if (request.description() != null) {
            incident.setDescription(request.description());
        }
        incident.setUpdatedAt(Instant.now());

        Incident saved = incidentRepository.save(incident);
        auditLogRepository.save(new IncidentAuditLog(saved.getId(), requesterId, IncidentAuditLog.Action.UPDATED));
        return saved;
    }

    @Transactional
    public void delete(UUID incidentId, UUID requesterId) {
        Incident incident = getOwnedIncidentOrThrow(incidentId, requesterId);
        assertWithinEditWindow(incident);

        incidentRepository.delete(incident);
        auditLogRepository.save(new IncidentAuditLog(incidentId, requesterId, IncidentAuditLog.Action.DELETED));
    }

    private Incident getOwnedIncidentOrThrow(UUID incidentId, UUID requesterId) {
        Incident incident = getById(incidentId);
        if (!incident.getAuthorId().equals(requesterId)) {
            throw new ForbiddenOperationException("Solo el autor del reporte puede modificarlo o eliminarlo");
        }
        return incident;
    }

    private void assertWithinEditWindow(Incident incident) {
        if (!incident.isWithinEditWindow(Instant.now(), editWindowMinutes)) {
            throw new EditWindowClosedException();
        }
    }
}
