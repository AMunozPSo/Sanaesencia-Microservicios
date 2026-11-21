package cl.sanaesencia.reservas.repository;
import cl.sanaesencia.reservas.model.BloqueoAgenda;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface BloqueoAgendaRepository extends JpaRepository<BloqueoAgenda,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BloqueoAgenda b where b.id=1")
    Optional<BloqueoAgenda> bloquear();
}
