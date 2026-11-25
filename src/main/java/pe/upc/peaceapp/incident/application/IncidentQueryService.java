package pe.upc.peaceapp.incident.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.upc.peaceapp.incident.api.dto.QueryResponses.HeatCellResponse;
import pe.upc.peaceapp.incident.domain.model.Incident;
import pe.upc.peaceapp.incident.domain.model.Severity;
import pe.upc.peaceapp.incident.domain.repository.IncidentFilter;
import pe.upc.peaceapp.incident.domain.repository.IncidentRepository;
import pe.upc.peaceapp.incident.domain.repository.IncidentSpecifications;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class IncidentQueryService {
    private static final Set<String> SORTABLE = Set.of("occurredAt", "createdAt");
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_MAP_POINTS = 1000;
    private static final double MIN_CELL_DEGREES = 0.001;
    private static final int MAX_HEAT_ROWS = 20_000;

    private final IncidentRepository incidents;

    public IncidentQueryService(IncidentRepository incidents) {
        this.incidents = incidents;
    }

    public Page<Incident> search(IncidentFilter filter, String sortBy, String direction, int page, int size) {
        var property = sortBy != null && SORTABLE.contains(sortBy) ? sortBy : "occurredAt";
        var order = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(order, property));
        return incidents.findAll(IncidentSpecifications.matching(filter), pageable);
    }

    public List<Incident> mapPoints(IncidentFilter filter, int limit) {
        var pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), MAX_MAP_POINTS), Sort.by(Sort.Direction.DESC, "occurredAt"));
        return incidents.findAll(IncidentSpecifications.matching(filter), pageable).getContent();
    }

    public List<HeatCellResponse> heatmap(IncidentFilter.Bounds bounds, double cellSize) {
        double cell = Math.max(cellSize, MIN_CELL_DEGREES);
        var counts = new java.util.HashMap<java.util.List<Long>, Long>();
        for (Object[] row : incidents.coordinatesInBounds(bounds.minLat(), bounds.maxLat(), bounds.minLng(), bounds.maxLng(),
                PageRequest.of(0, MAX_HEAT_ROWS))) {
            long latCell = (long) Math.floor(((Number) row[0]).doubleValue() / cell);
            long lngCell = (long) Math.floor(((Number) row[1]).doubleValue() / cell);
            counts.merge(java.util.List.of(latCell, lngCell), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .map(e -> new HeatCellResponse((e.getKey().get(0) + 0.5) * cell, (e.getKey().get(1) + 0.5) * cell,
                        e.getValue(), riskOf(e.getValue())))
                .toList();
    }

    public Stats stats() {
        var byStatus = new java.util.EnumMap<pe.upc.peaceapp.incident.domain.model.IncidentStatus, Long>(pe.upc.peaceapp.incident.domain.model.IncidentStatus.class);
        for (var s : pe.upc.peaceapp.incident.domain.model.IncidentStatus.values()) byStatus.put(s, 0L);
        incidents.countByStatus().forEach(row -> byStatus.put((pe.upc.peaceapp.incident.domain.model.IncidentStatus) row[0], ((Number) row[1]).longValue()));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long validated = byStatus.get(pe.upc.peaceapp.incident.domain.model.IncidentStatus.VALIDADO);
        return new Stats(total, total == 0 ? 0 : (double) validated / total, byStatus);
    }

    public record Stats(long total, double verifiedRate, java.util.Map<pe.upc.peaceapp.incident.domain.model.IncidentStatus, Long> byStatus) {
    }

    static Severity riskOf(long count) {
        if (count >= 10) return Severity.HIGH;
        if (count >= 4) return Severity.MEDIUM;
        return Severity.LOW;
    }
}
