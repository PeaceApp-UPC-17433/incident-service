package pe.upc.peaceapp.incident.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.upc.peaceapp.incident.api.dto.CreateIncidentRequest;
import pe.upc.peaceapp.incident.api.dto.IncidentResponse;
import pe.upc.peaceapp.incident.api.dto.UpdateIncidentRequest;
import pe.upc.peaceapp.incident.api.dto.IncidentSummaryResponse;
import pe.upc.peaceapp.incident.api.dto.QueryResponses.*;
import pe.upc.peaceapp.incident.application.IncidentQueryService;
import pe.upc.peaceapp.incident.application.IncidentService;
import pe.upc.peaceapp.incident.domain.model.IncidentStatus;
import pe.upc.peaceapp.incident.domain.model.IncidentType;
import pe.upc.peaceapp.incident.domain.model.Severity;
import pe.upc.peaceapp.incident.domain.repository.IncidentFilter;
import pe.upc.peaceapp.incident.evidence.EvidenceStorage;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentQueryService queryService;
    private final EvidenceStorage evidenceStorage;

    public IncidentController(IncidentService incidentService, IncidentQueryService queryService,
                              EvidenceStorage evidenceStorage) {
        this.incidentService = incidentService;
        this.queryService = queryService;
        this.evidenceStorage = evidenceStorage;
    }

    private IncidentResponse present(pe.upc.peaceapp.incident.domain.model.Incident incident) {
        return IncidentResponse.from(incident).withEvidenceUrls(evidenceStorage::signedReadUrl);
    }

    /** US-04, US-06, US-07, US-08: registro rapido de un incidente. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse create(Authentication authentication, @Valid @RequestBody CreateIncidentRequest request) {
        var incident = incidentService.create(currentUserId(authentication), request);
        return present(incident);
    }

    @GetMapping
    public PageResponse<IncidentSummaryResponse> list(Authentication authentication,
            @RequestParam(required = false) IncidentType type, @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) IncidentStatus status, @RequestParam(required = false) String q,
            @RequestParam(required = false) java.time.Instant from, @RequestParam(required = false) java.time.Instant to,
            @RequestParam(required = false) Boolean mine, @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        var author = Boolean.TRUE.equals(mine) ? currentUserId(authentication) : null;
        var filter = new IncidentFilter(type, severity, status, q, from, to, author, null, false);
        return PageResponse.of(queryService.search(filter, sortBy, direction, page, size), IncidentSummaryResponse::from);
    }

    @GetMapping("/stats")
    public IncidentQueryService.Stats stats() {
        return queryService.stats();
    }

    @GetMapping("/map")
    public java.util.List<MapPointResponse> map(@RequestParam double minLat, @RequestParam double maxLat,
            @RequestParam double minLng, @RequestParam double maxLng, @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) Severity severity, @RequestParam(defaultValue = "500") int limit) {
        var filter = new IncidentFilter(type, severity, null, null, null, null, null,
                new IncidentFilter.Bounds(minLat, maxLat, minLng, maxLng), true);
        return queryService.mapPoints(filter, limit).stream()
                .map(i -> new MapPointResponse(i.getId(), i.getType(), i.getType().severity(), i.getLatitude(), i.getLongitude()))
                .toList();
    }

    @GetMapping("/heatmap")
    public java.util.List<HeatCellResponse> heatmap(@RequestParam double minLat, @RequestParam double maxLat,
            @RequestParam double minLng, @RequestParam double maxLng, @RequestParam(defaultValue = "0.01") double cellSize) {
        return queryService.heatmap(new IncidentFilter.Bounds(minLat, maxLat, minLng, maxLng), cellSize);
    }

    @PutMapping("/{id}/status")
    public IncidentResponse changeStatus(Authentication authentication, @PathVariable UUID id,
                                         @Valid @RequestBody ChangeStatusRequest request) {
        return present(incidentService.changeStatus(id, currentUserId(authentication), request.status()));
    }

    /** US-21: detalle del incidente. */
    @GetMapping("/{id}")
    public IncidentResponse getById(@PathVariable UUID id) {
        return present(incidentService.getById(id));
    }

    /** US-10: historial de reportes propios. */
    @GetMapping("/mine")
    public java.util.List<IncidentResponse> getOwnHistory(Authentication authentication) {
        return incidentService.getOwnHistory(currentUserId(authentication)).stream()
                .map(this::present)
                .toList();
    }

    /** US-09: edicion dentro de la ventana de tiempo. */
    @PutMapping("/{id}")
    public IncidentResponse update(Authentication authentication, @PathVariable UUID id,
                                    @RequestBody UpdateIncidentRequest request) {
        var incident = incidentService.update(id, currentUserId(authentication), request);
        return present(incident);
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
