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
}
