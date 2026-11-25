package pe.upc.peaceapp.incident.domain.repository;

import pe.upc.peaceapp.incident.domain.model.Incident;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    @Query("select i.latitude, i.longitude from Incident i "
            + "where i.latitude between :minLat and :maxLat and i.longitude between :minLng and :maxLng "
            + "and i.status <> pe.upc.peaceapp.incident.domain.model.IncidentStatus.DESCARTADO")
    List<Object[]> coordinatesInBounds(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                       @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                                       org.springframework.data.domain.Pageable limit);

    @Query("select i.status, count(i) from Incident i group by i.status")
    List<Object[]> countByStatus();

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
