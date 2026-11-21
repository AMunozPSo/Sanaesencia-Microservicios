package cl.sanaesencia.reservas.repository;
import cl.sanaesencia.reservas.model.Box;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BoxRepository extends JpaRepository<Box,Long> {
    List<Box> findByActivoTrueOrderByIdAsc();
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
