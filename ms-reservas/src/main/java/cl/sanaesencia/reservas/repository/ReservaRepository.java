package cl.sanaesencia.reservas.repository;
import cl.sanaesencia.reservas.model.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReservaRepository extends JpaRepository<Reserva,Long> {
    List<Reserva> findByEstadoAndInicioLessThanAndFinGreaterThan(
            EstadoReserva estado, LocalDateTime fin, LocalDateTime inicio);
    List<Reserva> findByInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(LocalDateTime desde, LocalDateTime hasta);
    List<Reserva> findByEspecialistaIdAndEstadoAndInicioGreaterThanEqual(Long id, EstadoReserva estado, LocalDateTime desde);
    boolean existsByBox_IdAndEstadoAndFinAfter(Long id, EstadoReserva estado, LocalDateTime ahora);
}
