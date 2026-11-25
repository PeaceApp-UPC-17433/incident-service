package pe.upc.peaceapp.incident.domain.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pe.upc.peaceapp.incident.domain.model.Incident;
import pe.upc.peaceapp.incident.domain.model.IncidentStatus;
import pe.upc.peaceapp.incident.domain.model.IncidentType;

import java.util.ArrayList;
import java.util.Locale;

public final class IncidentSpecifications {
    private IncidentSpecifications() {
    }

    public static Specification<Incident> matching(IncidentFilter f) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (f.type() != null) predicates.add(cb.equal(root.get("type"), f.type()));
            if (f.severity() != null) predicates.add(root.get("type").in(IncidentType.withSeverity(f.severity())));
            if (f.status() != null) predicates.add(cb.equal(root.get("status"), f.status()));
            if (f.excludeDiscarded() && f.status() == null) predicates.add(cb.notEqual(root.get("status"), IncidentStatus.DESCARTADO));
            if (f.from() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), f.from()));
            if (f.to() != null) predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), f.to()));
            if (f.authorId() != null) predicates.add(cb.equal(root.get("authorId"), f.authorId()));
            if (f.text() != null && !f.text().isBlank()) {
                String like = "%" + f.text().trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("address")), like)));
            }
            if (f.bounds() != null) {
                var b = f.bounds();
                predicates.add(cb.between(root.get("latitude"), b.minLat(), b.maxLat()));
                predicates.add(cb.between(root.get("longitude"), b.minLng(), b.maxLng()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
