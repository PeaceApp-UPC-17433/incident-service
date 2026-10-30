package pe.upc.peaceapp.incident.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.upc.peaceapp.incident.api.dto.CreateIncidentRequest;
import pe.upc.peaceapp.incident.api.dto.UpdateIncidentRequest;
import pe.upc.peaceapp.incident.api.error.EditWindowClosedException;
import pe.upc.peaceapp.incident.api.error.ForbiddenOperationException;
import pe.upc.peaceapp.incident.domain.model.Incident;
import pe.upc.peaceapp.incident.domain.model.IncidentType;
import pe.upc.peaceapp.incident.domain.repository.IncidentAuditLogRepository;
import pe.upc.peaceapp.incident.domain.repository.IncidentRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    private static final long EDIT_WINDOW_MINUTES = 30;
    private static final long MAX_EVIDENCE_SIZE_BYTES = 25L * 1024 * 1024;

    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private IncidentAuditLogRepository auditLogRepository;

    private IncidentService newIncidentService() {
        return new IncidentService(incidentRepository, auditLogRepository, EDIT_WINDOW_MINUTES, MAX_EVIDENCE_SIZE_BYTES);
    }

    @Test
    void createPersistsIncidentWithAuthor() {
        IncidentService service = newIncidentService();
        UUID authorId = UUID.randomUUID();
        var request = new CreateIncidentRequest(IncidentType.ROBO, "Robo al paso", -12.05, -77.03, false, null);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Incident created = service.create(authorId, request);

        assertThat(created.getAuthorId()).isEqualTo(authorId);
        assertThat(created.getType()).isEqualTo(IncidentType.ROBO);
    }

    @Test
    void updateFailsWhenRequesterIsNotTheAuthor() {
        IncidentService service = newIncidentService();
        UUID authorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Incident incident = new Incident(authorId, false, IncidentType.ROBO, "desc", -12.05, -77.03);
        when(incidentRepository.findById(any())).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), otherUserId,
                new UpdateIncidentRequest(IncidentType.HURTO, "editado")))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deleteFailsWhenEditWindowExpired() throws Exception {
        IncidentService service = newIncidentService();
        UUID authorId = UUID.randomUUID();
        Incident incident = new Incident(authorId, false, IncidentType.ROBO, "desc", -12.05, -77.03);
        setCreatedAt(incident, Instant.now().minusSeconds((EDIT_WINDOW_MINUTES + 5) * 60));
        when(incidentRepository.findById(any())).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> service.delete(UUID.randomUUID(), authorId))
                .isInstanceOf(EditWindowClosedException.class);
    }

    private void setCreatedAt(Incident incident, Instant createdAt) throws Exception {
        var field = Incident.class.getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(incident, createdAt);
    }

    @Test
    void rejectsOversizeEvidenceWithoutPersistingIncident() {
        var request = new CreateIncidentRequest(IncidentType.ROBO, "desc", -12.05, -77.03, false,
                java.util.List.of(new pe.upc.peaceapp.incident.api.dto.EvidenceRequest(
                        pe.upc.peaceapp.incident.domain.model.Evidence.EvidenceType.FOTO,
                        "https://example.test/evidence.jpg", MAX_EVIDENCE_SIZE_BYTES + 1)));
        assertThatThrownBy(() -> newIncidentService().create(UUID.randomUUID(), request))
                .isInstanceOf(pe.upc.peaceapp.incident.api.error.EvidenceTooLargeException.class);
        org.mockito.Mockito.verifyNoInteractions(incidentRepository, auditLogRepository);
    }

    @Test
    void acceptsEvidenceAtTheSizeLimit() {
        when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var request = new CreateIncidentRequest(IncidentType.ROBO, "desc", -12.05, -77.03, false,
                java.util.List.of(new pe.upc.peaceapp.incident.api.dto.EvidenceRequest(
                        pe.upc.peaceapp.incident.domain.model.Evidence.EvidenceType.FOTO,
                        "https://example.test/evidence.jpg", MAX_EVIDENCE_SIZE_BYTES)));
        assertThat(newIncidentService().create(UUID.randomUUID(), request).getEvidences()).hasSize(1);
        org.mockito.Mockito.verify(auditLogRepository).save(any());
    }

    @Test
    void updatesOwnedIncidentAndAuditsTheChange() {
        UUID author = UUID.randomUUID();
        Incident incident = new Incident(author, false, IncidentType.ROBO, "antes", -12.05, -77.03);
        when(incidentRepository.findById(any())).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Incident result = newIncidentService().update(UUID.randomUUID(), author,
                new UpdateIncidentRequest(IncidentType.HURTO, "despues"));
        assertThat(result.getType()).isEqualTo(IncidentType.HURTO);
        assertThat(result.getDescription()).isEqualTo("despues");
        assertThat(result.getUpdatedAt()).isNotNull();
        org.mockito.Mockito.verify(auditLogRepository).save(any());
    }

    @Test
    void partialUpdatePreservesUnspecifiedFields() {
        UUID author = UUID.randomUUID();
        Incident incident = new Incident(author, false, IncidentType.ROBO, "original", -12.05, -77.03);
        when(incidentRepository.findById(any())).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Incident result = newIncidentService().update(UUID.randomUUID(), author, new UpdateIncidentRequest(null, null));
        assertThat(result.getType()).isEqualTo(IncidentType.ROBO);
        assertThat(result.getDescription()).isEqualTo("original");
    }

    @Test
    void deletesOwnedIncidentAndCreatesAuditRecord() {
        UUID author = UUID.randomUUID();
        Incident incident = new Incident(author, false, IncidentType.ROBO, "desc", -12.05, -77.03);
        when(incidentRepository.findById(any())).thenReturn(Optional.of(incident));
        newIncidentService().delete(UUID.randomUUID(), author);
        org.mockito.Mockito.verify(incidentRepository).delete(incident);
        org.mockito.Mockito.verify(auditLogRepository).save(any());
    }

    @Test
    void missingIncidentFailsBeforeMutation() {
        when(incidentRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> newIncidentService().getById(UUID.randomUUID()))
                .isInstanceOf(pe.upc.peaceapp.incident.api.error.IncidentNotFoundException.class);
    }

    @Test
    void historyIsScopedToTheAuthor() {
        UUID author = UUID.randomUUID();
        Incident incident = new Incident(author, false, IncidentType.ROBO, "desc", -12.05, -77.03);
        when(incidentRepository.findByAuthorIdOrderByCreatedAtDesc(author)).thenReturn(java.util.List.of(incident));
        assertThat(newIncidentService().getOwnHistory(author)).containsExactly(incident);
    }
}

