package cl.sanaesencia.reservas.config;
import cl.sanaesencia.reservas.model.*;
import cl.sanaesencia.reservas.repository.*;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class DatosIniciales implements ApplicationRunner {
    private final BloqueoAgendaRepository bloqueo;
    private final BoxRepository boxes;
    public DatosIniciales(BloqueoAgendaRepository bloqueo, BoxRepository boxes) {
        this.bloqueo=bloqueo; this.boxes=boxes;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if (!bloqueo.existsById(1L)) bloqueo.saveAndFlush(new BloqueoAgenda(1L));
        bloqueo.bloquear().orElseThrow();
        if (boxes.count()==0) for (int i=1;i<=3;i++) boxes.save(new Box("Box "+i));
    }
}
