package pe.upc.peaceapp.incident.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.upc.peaceapp.incident.messaging.IntegrationEventPublisher;
import pe.upc.peaceapp.incident.domain.model.IncidentStatus;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.incident.api.dto.CreateIncidentRequest;
import pe.upc.peaceapp.incident.api.dto.EvidenceRequest;
import pe.upc.peaceapp.incident.api.dto.UpdateIncidentRequest;
import pe.upc.peaceapp.incident.api.error.EditWindowClosedException;
import pe.upc.peaceapp.incident.api.error.EvidenceTooLargeException;
import pe.upc.peaceapp.incident.api.error.ForbiddenOperationException;
import pe.upc.peaceapp.incident.api.error.IncidentNotFoundException;
import pe.upc.peaceapp.incident.api.error.InvalidStatusTransitionException;
import pe.upc.peaceapp.incident.domain.model.Evidence;
import pe.upc.peaceapp.incident.domain.model.Incident;
import pe.upc.peaceapp.incident.domain.model.IncidentAuditLog;
import pe.upc.peaceapp.incident.domain.repository.IncidentAuditLogRepository;
import pe.upc.peaceapp.incident.domain.repository.IncidentRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** BC-02: ciclo de vida del reporte (US-04, US-06 a US-10, US-21). */
@Service
public class IncidentService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentAuditLogRepository auditLogRepository;
    private final IntegrationEventPublisher events;
    private final long editWindowMinutes;
    private final long maxEvidenceSizeBytes;

    public IncidentService(IncidentRepository incidentRepository,
                            IncidentAuditLogRepository auditLogRepository,
                            IntegrationEventPublisher events,
                            @Value("${incident.edit-window-minutes}") long editWindowMinutes,
                            @Value("${incident.max-evidence-size-bytes}") long maxEvidenceSizeBytes) {
        this.incidentRepository = incidentRepository;
        this.auditLogRepository = auditLogRepository;
        this.events = events;
        this.editWindowMinutes = editWindowMinutes;
        this.maxEvidenceSizeBytes = maxEvidenceSizeBytes;
    }

    @Transactional
    public Incident create(UUID authorId, CreateIncidentRequest request) {
        Incident incident = new Incident(authorId, request.anonymous(), request.type(),
                request.description(), request.latitude(), request.longitude());
        incident.setTitle(blankToNull(request.title()));
        incident.setAddress(blankToNull(request.address()));
        if (request.occurredAt() != null) {
            if (request.occurredAt().isAfter(Instant.now().plusSeconds(300))) {
                throw new IllegalArgumentException("La fecha del hecho no puede estar en el futuro");
            }
            incident.setOccurredAt(request.occurredAt());
        }

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
        events.publish("incident.created", incidentCreatedEvent(saved));
        log.info("Incidente creado incidentId={} authorId={} tipo={} anonimo={}", saved.getId(), authorId, saved.getType(), saved.isAnonymous());
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
        if (request.title() != null) {
            incident.setTitle(blankToNull(request.title()));
        }
        if (request.address() != null) {
            incident.setAddress(blankToNull(request.address()));
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
        events.publish("incident.deleted", baseEvent(incidentId));
        log.info("Incidente eliminado incidentId={} requesterId={}", incidentId, requesterId);
    }

    @Transactional
    public boolean applyExternalStatus(UUID incidentId, IncidentStatus target) {
        return incidentRepository.findById(incidentId).map(incident -> {
            if (incident.getStatus() == target || incident.getStatus() == IncidentStatus.DESCARTADO) return false;
            incident.setStatus(target);
            incident.setUpdatedAt(Instant.now());
            incidentRepository.save(incident);
            return true;
        }).orElse(false);
    }

    @Transactional
    public Incident changeStatus(UUID incidentId, UUID moderatorId, IncidentStatus target) {
        Incident incident = getById(incidentId);
        if (!isAllowedTransition(incident.getStatus(), target)) {
            throw new InvalidStatusTransitionException(incident.getStatus(), target);
        }
        incident.setStatus(target);
        incident.setUpdatedAt(Instant.now());
        Incident saved = incidentRepository.save(incident);
        log.info("Estado cambiado manualmente incidentId={} moderatorId={} a={}", incidentId, moderatorId, target);
        return saved;
    }

    static boolean isAllowedTransition(IncidentStatus from, IncidentStatus to) {
        return switch (from) {
            case PENDIENTE_VALIDACION -> to == IncidentStatus.EN_MODERACION || to == IncidentStatus.VALIDADO || to == IncidentStatus.DESCARTADO;
            case EN_MODERACION -> to == IncidentStatus.VALIDADO || to == IncidentStatus.DESCARTADO;
            case VALIDADO -> to == IncidentStatus.DESCARTADO;
            case DESCARTADO -> false;
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LinkedHashMap<String, Object> baseEvent(UUID incidentId) {
        var event = new LinkedHashMap<String, Object>();
        event.put("eventId", UUID.randomUUID());
        event.put("occurredAt", Instant.now().toString());
        event.put("incidentId", incidentId);
        return event;
    }

    private static LinkedHashMap<String, Object> incidentCreatedEvent(Incident incident) {
        var event = baseEvent(incident.getId());
        event.put("authorId", incident.getAuthorId());
        event.put("type", incident.getType().name());
        event.put("latitude", incident.getLatitude());
        event.put("longitude", incident.getLongitude());
        event.put("severity", incident.getType().severity().name());
        event.put("description", incident.getDescription());
        event.put("hasEvidence", !incident.getEvidences().isEmpty());
        return event;
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
