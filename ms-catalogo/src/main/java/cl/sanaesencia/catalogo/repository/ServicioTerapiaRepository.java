package cl.sanaesencia.catalogo.repository;

import cl.sanaesencia.catalogo.model.ServicioTerapia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicioTerapiaRepository
        extends JpaRepository<ServicioTerapia, Long> {

    List<ServicioTerapia> findByActivoTrue();

    boolean existsByNombreIgnoreCase(String nombre);
    Optional<ServicioTerapia> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(
            String nombre,
            Long id
    );
}
