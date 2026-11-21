package cl.sanaesencia.reservas.service;
import cl.sanaesencia.reservas.client.CatalogoClient;
import cl.sanaesencia.reservas.dto.*;
import cl.sanaesencia.reservas.model.*;
import cl.sanaesencia.reservas.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import static cl.sanaesencia.reservas.service.ReglasAgenda.error;
@Service
public class AgendaService {
    private final HorarioRepository horarios;
    private final ReservaRepository reservas;
    private final ReglasAgenda reglas;
    private final CatalogoClient catalogo;
    private final Clock reloj;
    private final int paso;
    public AgendaService(HorarioRepository horarios,ReservaRepository reservas,ReglasAgenda reglas,
            CatalogoClient catalogo,Clock reloj,@Value("${agenda.paso-minutos}") int paso) {
        this.horarios=horarios; this.reservas=reservas; this.reglas=reglas;
        this.catalogo=catalogo; this.reloj=reloj;
        if(paso<=0||60%paso!=0) throw new IllegalArgumentException("El paso debe ser un divisor positivo de 60");
        this.paso=paso;
    }
    @Transactional(readOnly=true)
    public List<AgendaRequest.Franja> consultar(Long especialistaId) {
        catalogo.perfil(especialistaId);
        return horarios.findByEspecialistaIdOrderByDiaAscDesdeAsc(especialistaId).stream()
            .map(h->new AgendaRequest.Franja(h.dia,h.desde,h.hasta)).toList();
    }
    @Transactional
    public List<AgendaRequest.Franja> guardar(Long especialistaId,AgendaRequest solicitud,Cuenta cuenta) {
        reglas.administrar(cuenta,especialistaId);
        catalogo.perfil(especialistaId);
        List<AgendaRequest.Franja> franjas=solicitud.horarios().stream()
            .sorted(Comparator.comparing(AgendaRequest.Franja::dia).thenComparing(AgendaRequest.Franja::desde)).toList();
        for(int i=0;i<franjas.size();i++) {
            var actual=franjas.get(i);
            if(!actual.desde().isBefore(actual.hasta())||!enGrilla(actual.desde())||!enGrilla(actual.hasta()))
                throw error(HttpStatus.BAD_REQUEST,"Las franjas deben ser crecientes y usar intervalos de "+paso+" minutos");
            if(i>0&&actual.dia()==franjas.get(i-1).dia()&&actual.desde().isBefore(franjas.get(i-1).hasta()))
                throw error(HttpStatus.BAD_REQUEST,"Los horarios no pueden superponerse");
        }
        reglas.bloquear();
        var pendientes=reservas.findByEspecialistaIdAndEstadoAndInicioGreaterThanEqual(especialistaId,
            EstadoReserva.CONFIRMADA,LocalDateTime.now(reloj));
        for(var reserva:pendientes) if(franjas.stream().noneMatch(h->cubre(h,reserva.inicio,reserva.fin)))
            throw error(HttpStatus.CONFLICT,"El cambio dejaría una reserva confirmada fuera de la agenda");
        horarios.deleteByEspecialistaId(especialistaId);
        horarios.flush();
        for(var franja:franjas) {
            Horario horario=new Horario();
            horario.especialistaId=especialistaId; horario.dia=franja.dia();
            horario.desde=franja.desde(); horario.hasta=franja.hasta();
            horarios.save(horario);
        }
        return franjas;
    }
    public boolean enGrilla(LocalTime hora) {
        return hora.getSecond()==0&&hora.getNano()==0&&hora.getMinute()%paso==0;
    }
    public int pasoMinutos() { return paso; }
    public boolean cubre(AgendaRequest.Franja franja,LocalDateTime inicio,LocalDateTime fin) {
        return inicio.toLocalDate().equals(fin.toLocalDate())&&inicio.getDayOfWeek()==franja.dia()
            &&!inicio.toLocalTime().isBefore(franja.desde())&&!fin.toLocalTime().isAfter(franja.hasta());
    }
    public boolean admite(Long especialistaId,LocalDateTime inicio,LocalDateTime fin) {
        return horarios.findByEspecialistaIdAndDiaOrderByDesdeAsc(especialistaId,inicio.getDayOfWeek())
            .stream().anyMatch(h->cubre(new AgendaRequest.Franja(h.dia,h.desde,h.hasta),inicio,fin));
    }
}
