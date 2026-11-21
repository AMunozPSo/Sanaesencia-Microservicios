package cl.sanaesencia.reservas.service;
import cl.sanaesencia.reservas.dto.Cuenta;
import cl.sanaesencia.reservas.model.*;
import cl.sanaesencia.reservas.repository.BloqueoAgendaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
@Component
public class ReglasAgenda {
    private final BloqueoAgendaRepository bloqueo;
    public ReglasAgenda(BloqueoAgendaRepository bloqueo) { this.bloqueo=bloqueo; }
    public void bloquear() {
        bloqueo.bloquear().orElseThrow(()->error(HttpStatus.SERVICE_UNAVAILABLE,"Agenda no inicializada"));
    }
    public void administrar(Cuenta cuenta,Long especialistaId) {
        if(!cuenta.esAdmin() && !(cuenta.esEspecialista()&&especialistaId.equals(cuenta.especialistaId())))
            throw error(HttpStatus.FORBIDDEN,"Solo puedes gestionar tu propia agenda");
    }
    public void ver(Cuenta cuenta,Reserva reserva) {
        if(!cuenta.esAdmin() && !(cuenta.esPaciente()&&cuenta.id().equals(reserva.pacienteId))
                && !(cuenta.esEspecialista()&&reserva.especialistaId.equals(cuenta.especialistaId())))
            throw error(HttpStatus.FORBIDDEN,"No puedes consultar reservas de otra persona");
    }
    public static ResponseStatusException error(HttpStatus estado,String mensaje) {
        return new ResponseStatusException(estado,mensaje);
    }
}
