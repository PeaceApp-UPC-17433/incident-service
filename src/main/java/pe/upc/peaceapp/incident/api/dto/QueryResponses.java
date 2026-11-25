package pe.upc.peaceapp.incident.api.dto;

import org.springframework.data.domain.Page;
import pe.upc.peaceapp.incident.domain.model.IncidentType;
import pe.upc.peaceapp.incident.domain.model.Severity;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class QueryResponses {
    private QueryResponses() {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }

    public record MapPointResponse(UUID id, IncidentType type, Severity severity, double latitude, double longitude) {
    }

    public record HeatCellResponse(double latitude, double longitude, long count, Severity risk) {
    }

    public record ChangeStatusRequest(@jakarta.validation.constraints.NotNull pe.upc.peaceapp.incident.domain.model.IncidentStatus status) {
    }
}
