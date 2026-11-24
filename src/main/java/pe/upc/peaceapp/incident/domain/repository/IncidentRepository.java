package pe.upc.peaceapp.incident.domain.repository;

import pe.upc.peaceapp.incident.domain.model.Incident;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    /**
     * Las evidencias se cargan junto con el incidente: con open-in-view desactivado, el controlador
     * arma la respuesta fuera de la transaccion y una coleccion perezosa lanzaria LazyInitializationException.
     */
    @EntityGraph(attributePaths = "evidences")
    List<Incident> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    @Override
    @EntityGraph(attributePaths = "evidences")
    Optional<Incident> findById(UUID id);
}
