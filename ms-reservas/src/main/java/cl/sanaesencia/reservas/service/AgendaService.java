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
            .map(h->new AgendaRequest.Franja(h.dia,h.desde,h.hasta,h.fecha,h.cupoPublicado)).toList();
    }
    @Transactional
    public List<AgendaRequest.Franja> guardar(Long especialistaId,AgendaRequest solicitud,Cuenta cuenta) {
        reglas.administrar(cuenta,especialistaId);
        catalogo.perfil(especialistaId);
        List<AgendaRequest.Franja> franjas=solicitud.horarios().stream()
            .sorted(Comparator.comparing(AgendaRequest.Franja::dia).thenComparing(AgendaRequest.Franja::desde)).toList();
        for(int i=0;i<franjas.size();i++) {
            var actual=franjas.get(i);
            if(actual.fecha()!=null&&actual.fecha().getDayOfWeek()!=actual.dia())
                throw error(HttpStatus.BAD_REQUEST,"La fecha debe corresponder al día de la franja");
            if(actual.cupoPublicado()&&actual.fecha()==null)
                throw error(HttpStatus.BAD_REQUEST,"Un cupo publicado requiere una fecha concreta");
            if(!actual.cupoPublicado()&&(actual.dia()==DayOfWeek.SATURDAY||actual.dia()==DayOfWeek.SUNDAY
                    ||actual.desde().isBefore(LocalTime.of(9,0))||actual.hasta().isAfter(LocalTime.of(19,0))))
                throw error(HttpStatus.BAD_REQUEST,"La atención es de lunes a viernes de 09:00 a 19:00, con última cita a las 18:00");
            if(!actual.desde().isBefore(actual.hasta())||actual.desde().getSecond()!=0||actual.hasta().getSecond()!=0
                    ||(!actual.cupoPublicado()&&(!enGrilla(actual.desde())||!enGrilla(actual.hasta()))))
                throw error(HttpStatus.BAD_REQUEST,"Las franjas deben ser crecientes y usar intervalos de "+paso+" minutos");
            for(int j=0;j<i;j++) {
                var previa=franjas.get(j);
                boolean mismosDias=actual.dia()==previa.dia()&&(actual.fecha()==null||previa.fecha()==null||actual.fecha().equals(previa.fecha()));
                if(mismosDias&&(actual.cupoPublicado()&&previa.cupoPublicado()?actual.desde().equals(previa.desde())
                    :actual.desde().isBefore(previa.hasta())&&actual.hasta().isAfter(previa.desde())))
                    throw error(HttpStatus.BAD_REQUEST,"Los horarios no pueden superponerse");
            }
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
            horario.desde=franja.desde(); horario.hasta=franja.hasta();horario.fecha=franja.fecha();horario.cupoPublicado=franja.cupoPublicado();
            horarios.save(horario);
        }
        return franjas;
    }
    public boolean enGrilla(LocalTime hora) {
        return hora.getSecond()==0&&hora.getNano()==0&&hora.getMinute()%paso==0;
    }
    public int pasoMinutos() { return paso; }
    public boolean cubre(AgendaRequest.Franja franja,LocalDateTime inicio,LocalDateTime fin) {
        return (franja.fecha()==null||franja.fecha().equals(inicio.toLocalDate()))
            &&(!franja.cupoPublicado()||inicio.toLocalTime().equals(franja.desde()))
            &&inicio.toLocalDate().equals(fin.toLocalDate())&&inicio.getDayOfWeek()==franja.dia()
            &&!inicio.toLocalTime().isBefore(franja.desde())&&!fin.toLocalTime().isAfter(franja.hasta());
    }
    public boolean admite(Long especialistaId,LocalDateTime inicio,LocalDateTime fin) {
        var franjas=horarios.findByEspecialistaIdAndDiaOrderByDesdeAsc(especialistaId,inicio.getDayOfWeek());
        if(franjas.stream().filter(h->h.cupoPublicado).anyMatch(h->cubre(new AgendaRequest.Franja(h.dia,h.desde,h.hasta,h.fecha,true),inicio,fin)))return true;
        if(inicio.getDayOfWeek()==DayOfWeek.SATURDAY||inicio.getDayOfWeek()==DayOfWeek.SUNDAY
                ||inicio.toLocalTime().isBefore(LocalTime.of(9,0))||inicio.toLocalTime().isAfter(LocalTime.of(18,0))
                ||!inicio.toLocalDate().equals(fin.toLocalDate())||fin.toLocalTime().isAfter(LocalTime.of(19,0))) return false;
        return franjas.stream().filter(h->!h.cupoPublicado).anyMatch(h->cubre(new AgendaRequest.Franja(h.dia,h.desde,h.hasta,h.fecha),inicio,fin));
    }
    public boolean inicioPublicado(Long especialistaId,LocalDateTime inicio) {
        return horarios.findByEspecialistaIdAndDiaOrderByDesdeAsc(especialistaId,inicio.getDayOfWeek()).stream()
            .anyMatch(h->h.cupoPublicado&&inicio.toLocalDate().equals(h.fecha)&&inicio.toLocalTime().equals(h.desde));
    }
}
