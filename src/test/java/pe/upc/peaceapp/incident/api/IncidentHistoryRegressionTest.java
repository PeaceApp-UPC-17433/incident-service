package pe.upc.peaceapp.incident.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pe.upc.peaceapp.incident.api.dto.CreateIncidentRequest;
import pe.upc.peaceapp.incident.api.dto.EvidenceRequest;
import pe.upc.peaceapp.incident.api.dto.IncidentResponse;
import pe.upc.peaceapp.incident.application.IncidentService;
import pe.upc.peaceapp.incident.domain.model.Evidence;
import pe.upc.peaceapp.incident.domain.model.IncidentType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regresion del incidente detectado por la alerta PeaceAppHighErrorRate (seccion 4.3):
 * GET /api/v1/incidents/mine respondia 500 por LazyInitializationException al leer las evidencias
 * fuera de la transaccion (open-in-view: false).
 */
@SpringBootTest
class IncidentHistoryRegressionTest {

    @Autowired
    private IncidentService incidentService;

    @Test
    void ownHistoryAndDetailIncludeEvidencesOutsideTransaction() {
        UUID authorId = UUID.randomUUID();
        var request = new CreateIncidentRequest(IncidentType.ROBO, null, "Robo con evidencia", null, null, -12.05, -77.03, false,
                List.of(new EvidenceRequest(Evidence.EvidenceType.FOTO, "https://example.org/f.jpg", 1024)));
        UUID id = incidentService.create(authorId, request).getId();

        List<IncidentResponse> history = incidentService.getOwnHistory(authorId).stream()
                .map(IncidentResponse::from).toList();
        IncidentResponse detail = IncidentResponse.from(incidentService.getById(id));

        assertThat(history).hasSize(1);
        assertThat(history.get(0).evidences()).hasSize(1);
        assertThat(detail.evidences()).hasSize(1);
    }
}
