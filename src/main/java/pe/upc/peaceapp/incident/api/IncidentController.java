package pe.upc.peaceapp.incident.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.upc.peaceapp.incident.api.dto.CreateIncidentRequest;
import pe.upc.peaceapp.incident.api.dto.IncidentResponse;
import pe.upc.peaceapp.incident.api.dto.UpdateIncidentRequest;
import pe.upc.peaceapp.incident.application.IncidentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    /** US-04, US-06, US-07, US-08: registro rapido de un incidente. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse create(Authentication authentication, @Valid @RequestBody CreateIncidentRequest request) {
        var incident = incidentService.create(currentUserId(authentication), request);
        return IncidentResponse.from(incident);
    }

    /** US-21: detalle del incidente. */
    @GetMapping("/{id}")
    public IncidentResponse getById(@PathVariable UUID id) {
        return IncidentResponse.from(incidentService.getById(id));
    }

    /** US-10: historial de reportes propios. */
    @GetMapping("/mine")
    public java.util.List<IncidentResponse> getOwnHistory(Authentication authentication) {
        return incidentService.getOwnHistory(currentUserId(authentication)).stream()
                .map(IncidentResponse::from)
                .toList();
    }

    /** US-09: edicion dentro de la ventana de tiempo. */
    @PutMapping("/{id}")
    public IncidentResponse update(Authentication authentication, @PathVariable UUID id,
                                    @RequestBody UpdateIncidentRequest request) {
        var incident = incidentService.update(id, currentUserId(authentication), request);
        return IncidentResponse.from(incident);
    }

    /** US-09: eliminacion dentro de la ventana de tiempo. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        incidentService.delete(id, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
