package cl.sanaesencia.catalogo.repository;

import cl.sanaesencia.catalogo.model.Especialista;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EspecialistaRepository extends JpaRepository<Especialista, Long> {
    Optional<Especialista> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @EntityGraph(attributePaths = "servicios")
    List<Especialista> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = "servicios")
    List<Especialista> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "servicios")
    @Query("select distinct e from Especialista e join e.servicios s " +
            "where e.activo = true and s.activo = true and s.id = :servicioId " +
            "and (e.permitePresencial = true or (e.permiteOnline = true and s.permiteOnline = true)) " +
            "order by e.nombre")
    List<Especialista> buscarPorServicio(@Param("servicioId") Long servicioId);

    boolean existsByActivoTrueAndPermitePresencialFalseAndServicios_Id(Long servicioId);
}
