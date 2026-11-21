package cl.sanaesencia.reservas.repository;
import cl.sanaesencia.reservas.model.Horario;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HorarioRepository extends JpaRepository<Horario,Long> {
    List<Horario> findByEspecialistaIdOrderByDiaAscDesdeAsc(Long id);
    List<Horario> findByEspecialistaIdAndDiaOrderByDesdeAsc(Long id, DayOfWeek dia);
    void deleteByEspecialistaId(Long id);
}
