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
        return aggregate(incidents.coordinatesInBounds(bounds.minLat(), bounds.maxLat(), bounds.minLng(), bounds.maxLng(),
                PageRequest.of(0, MAX_HEAT_ROWS)), cell);
    }

    /**
     * Agrupa coordenadas en una cuadricula. Cada celda se ubica en el centroide de sus reportes reales, no en el
     * centro geometrico de la celda: asi el mapa de calor no dibuja una rejilla regular y las zonas se ven donde ocurren.
     */
    static List<HeatCellResponse> aggregate(List<Object[]> rows, double cell) {
        var cells = new java.util.HashMap<java.util.List<Long>, CellAccumulator>();
        for (Object[] row : rows) {
            double lat = ((Number) row[0]).doubleValue();
            double lng = ((Number) row[1]).doubleValue();
            var key = java.util.List.of((long) Math.floor(lat / cell), (long) Math.floor(lng / cell));
            cells.computeIfAbsent(key, k -> new CellAccumulator()).add(lat, lng);
        }
        return cells.values().stream()
                .map(a -> new HeatCellResponse(a.latSum / a.count, a.lngSum / a.count, a.count, riskOf(a.count)))
                .toList();
    }

    /** Suma coordenadas de una celda para calcular su centroide. Clase y no record: un record con arreglos compara por referencia. */
    private static final class CellAccumulator {
        private double latSum;
        private double lngSum;
        private long count;

        void add(double lat, double lng) {
            latSum += lat;
            lngSum += lng;
            count++;
        }
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
