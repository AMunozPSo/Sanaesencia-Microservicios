package cl.sanaesencia.reservas.service;

import cl.sanaesencia.reservas.client.CatalogoClient;
import cl.sanaesencia.reservas.dto.*;
import cl.sanaesencia.reservas.model.*;
import cl.sanaesencia.reservas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import static cl.sanaesencia.reservas.service.ReglasAgenda.error;

@Service
public class ReservaService {
    public record BoxDisponible(Long id,String nombre) {}
    public record Turno(LocalDateTime inicio,LocalDateTime fin,List<BoxDisponible> boxesDisponibles) {}
    public record Sugerencia(Double distanciaKm,boolean permitePresencial,boolean permiteOnline,
                             boolean sugiereOnline,String mensaje) {}
    private final ReservaRepository reservas;
    private final BoxRepository boxes;
    private final HorarioRepository horarios;
    private final ReglasAgenda reglas;
    private final AgendaService agenda;
    private final CatalogoClient catalogo;
    private final DistanciaService distancia;
    private final Clock reloj;
    public ReservaService(ReservaRepository reservas,BoxRepository boxes,HorarioRepository horarios,
            ReglasAgenda reglas,AgendaService agenda,CatalogoClient catalogo,DistanciaService distancia,Clock reloj) {
        this.reservas=reservas; this.boxes=boxes; this.horarios=horarios; this.reglas=reglas;
        this.agenda=agenda; this.catalogo=catalogo; this.distancia=distancia; this.reloj=reloj;
    }

    @Transactional
    public ReservaResponse crear(ReservaRequest solicitud,Cuenta cuenta) {
        if(!cuenta.esPaciente()) throw error(HttpStatus.FORBIDDEN,"Solo un paciente puede crear su reserva");
        var atencion=catalogo.atencion(solicitud.especialistaId(),solicitud.servicioId());
        reglas.bloquear();
        Reserva reserva=new Reserva();
        reserva.pacienteId=cuenta.id(); reserva.pacienteNombre=cuenta.nombreCompleto();
        reserva.creadaEn=LocalDateTime.now(reloj);
        aplicar(reserva,solicitud,atencion);
        return respuesta(reservas.saveAndFlush(reserva));
    }

    @Transactional
    public ReservaResponse reprogramar(Long id,ReservaRequest solicitud,Cuenta cuenta) {
        reglas.bloquear();
        Reserva reserva=buscar(id);
        reglas.ver(cuenta,reserva);
        if(!cuenta.esAdmin()&&!cuenta.esPaciente())
            throw error(HttpStatus.FORBIDDEN,"El especialista no puede reprogramar reservas del paciente");
        if(reserva.estado!=EstadoReserva.CONFIRMADA||!reserva.inicio.isAfter(LocalDateTime.now(reloj)))
            throw error(HttpStatus.CONFLICT,"Solo se puede reprogramar una reserva confirmada futura");
        aplicar(reserva,solicitud,catalogo.atencion(solicitud.especialistaId(),solicitud.servicioId()));
        return respuesta(reservas.saveAndFlush(reserva));
    }

    private void aplicar(Reserva reserva,ReservaRequest solicitud,CatalogoClient.Atencion atencion) {
        validarModalidad(atencion,solicitud.modalidad());
        LocalDateTime inicio=solicitud.inicio(),fin=inicio.plusMinutes(atencion.servicio().duracionMinutos());
        if(!inicio.isAfter(LocalDateTime.now(reloj)))
            throw error(HttpStatus.BAD_REQUEST,"La reserva debe ser futura");
        if(!agenda.enGrilla(inicio.toLocalTime())&&!agenda.inicioPublicado(solicitud.especialistaId(),inicio))
            throw error(HttpStatus.BAD_REQUEST,"El inicio debe usar intervalos de "+agenda.pasoMinutos()+" minutos, sin segundos");
        if(!agenda.admite(solicitud.especialistaId(),inicio,fin))
            throw error(HttpStatus.CONFLICT,"El horario no está dentro de la agenda del especialista");
        List<Reserva> ocupadas=solapadas(inicio,fin,reserva.id);
        if(ocupadas.stream().anyMatch(r->r.especialistaId.equals(solicitud.especialistaId())))
            throw error(HttpStatus.CONFLICT,"El especialista ya tiene una cita en ese horario");
        if(ocupadas.stream().anyMatch(r->r.pacienteId.equals(reserva.pacienteId)))
            throw error(HttpStatus.CONFLICT,"Ya tienes una cita que se superpone con ese horario");

        Box box=null;
        if(solicitud.modalidad()==Modalidad.PRESENCIAL) {
            List<Box> libres=boxesLibres(ocupadas);
            if(libres.isEmpty()) throw error(HttpStatus.CONFLICT,"No hay boxes disponibles en ese horario");
            box=libres.get(ThreadLocalRandom.current().nextInt(libres.size()));
        }
        reserva.servicioId=solicitud.servicioId(); reserva.servicioNombre=atencion.servicio().nombre();
        reserva.especialistaId=solicitud.especialistaId(); reserva.especialistaNombre=atencion.especialista().nombre();
        reserva.precio=atencion.servicio().precio();
        reserva.inicio=inicio; reserva.fin=fin; reserva.modalidad=solicitud.modalidad(); reserva.box=box;
        reserva.distanciaKm=distancia.calcular(solicitud.latitud(),solicitud.longitud());
        reserva.onlineDisponible=atencion.especialista().permiteOnline()&&atencion.servicio().permiteOnline();
    }

    @Transactional(readOnly=true)
    public List<Turno> disponibilidad(Long especialistaId,Long servicioId,LocalDate fecha,Modalidad modalidad,Cuenta cuenta,Long reservaId) {
        if(reservaId!=null) {
            Reserva propia=buscar(reservaId);
            reglas.ver(cuenta,propia);
            if(!cuenta.esPaciente()&&!cuenta.esAdmin())
                throw error(HttpStatus.FORBIDDEN,"Solo el paciente o admin puede reprogramar esta reserva");
            if(propia.estado!=EstadoReserva.CONFIRMADA||!propia.inicio.isAfter(LocalDateTime.now(reloj)))
                throw error(HttpStatus.CONFLICT,"Solo se puede reprogramar una reserva confirmada futura");
        }
        var atencion=catalogo.atencion(especialistaId,servicioId);
        validarModalidad(atencion,modalidad);
        if(fecha.isBefore(LocalDate.now(reloj))) throw error(HttpStatus.BAD_REQUEST,"La fecha no puede ser pasada");
        List<Turno> resultado=new ArrayList<>();
        for(var franja:horarios.findByEspecialistaIdAndDiaOrderByDesdeAsc(especialistaId,fecha.getDayOfWeek())) {
            if(franja.fecha!=null&&!franja.fecha.equals(fecha)) continue;
            LocalDateTime inicio=fecha.atTime(franja.desde);
            while(!inicio.plusMinutes(atencion.servicio().duracionMinutos()).isAfter(fecha.atTime(franja.hasta))) {
                LocalDateTime fin=inicio.plusMinutes(atencion.servicio().duracionMinutos());
                List<Reserva> ocupadas=solapadas(inicio,fin,reservaId);
                boolean ocupado=ocupadas.stream().anyMatch(r->r.especialistaId.equals(especialistaId)
                    ||(cuenta.esPaciente()&&r.pacienteId.equals(cuenta.id())));
                List<Box> libres=modalidad==Modalidad.PRESENCIAL?boxesLibres(ocupadas):List.of();
                if(inicio.isAfter(LocalDateTime.now(reloj))&&agenda.admite(especialistaId,inicio,fin)&&!ocupado&&(modalidad==Modalidad.ONLINE||!libres.isEmpty()))
                    resultado.add(new Turno(inicio,fin,libres.stream().map(b->new BoxDisponible(b.getId(),b.getNombre())).toList()));
                if(franja.cupoPublicado)break;
                inicio=inicio.plusMinutes(agenda.pasoMinutos());
            }
        }
        return resultado.stream().collect(java.util.stream.Collectors.toMap(Turno::inicio,t->t,(a,b)->a,java.util.TreeMap::new)).values().stream().toList();
    }

    public Sugerencia sugerir(Long especialistaId,Long servicioId,Double latitud,Double longitud) {
        var atencion=catalogo.atencion(especialistaId,servicioId);
        Double km=distancia.calcular(latitud,longitud);
        boolean online=atencion.especialista().permiteOnline()&&atencion.servicio().permiteOnline();
        boolean sugerir=km!=null&&km>distancia.umbral()&&online;
        String mensaje=sugerir?"Estás a más de 20 km. Puedes elegir una sesión online."
            :km==null?"Comparte tu ubicación para calcular la distancia."
            :!online?"Esta atención requiere modalidad presencial.":"Puedes elegir la modalidad disponible que prefieras.";
        return new Sugerencia(km,atencion.especialista().permitePresencial(),online,sugerir,mensaje);
    }

    @Transactional(readOnly=true)
    public List<ReservaResponse> listar(YearMonth mes,Cuenta cuenta) {
        if(cuenta.esEspecialista()&&cuenta.especialistaId()==null)
            throw error(HttpStatus.FORBIDDEN,"Tu cuenta aún no está vinculada a un perfil de especialista");
        return reservas.findByInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                mes.atDay(1).atStartOfDay(),mes.plusMonths(1).atDay(1).atStartOfDay()).stream()
            .filter(r->cuenta.esAdmin()||(cuenta.esPaciente()&&cuenta.id().equals(r.pacienteId))
                ||(cuenta.esEspecialista()&&r.especialistaId.equals(cuenta.especialistaId())))
            .map(this::respuesta).toList();
    }

    @Transactional(readOnly=true)
    public ReservaResponse consultar(Long id,Cuenta cuenta) {
        Reserva reserva=buscar(id); reglas.ver(cuenta,reserva); return respuesta(reserva);
    }

    @Transactional
    public ReservaResponse cambiarEstado(Long id,EstadoReserva estado,Cuenta cuenta) {
        reglas.bloquear();
        Reserva reserva=buscar(id); reglas.ver(cuenta,reserva);
        if(reserva.estado!=EstadoReserva.CONFIRMADA)
            throw error(HttpStatus.CONFLICT,"La reserva ya está cancelada o atendida");
        if(estado==EstadoReserva.CONFIRMADA)
            throw error(HttpStatus.BAD_REQUEST,"No se puede reiniciar una reserva mediante su estado");
        if(estado==EstadoReserva.ATENDIDA) {
            if(cuenta.esPaciente()) throw error(HttpStatus.FORBIDDEN,"Solo el especialista o admin puede marcar la atención");
            if(reserva.fin.isAfter(LocalDateTime.now(reloj)))
                throw error(HttpStatus.CONFLICT,"La sesión aún no ha terminado");
        } else if(reserva.inicio.isBefore(LocalDateTime.now(reloj).plusHours(12))) {
            throw error(HttpStatus.CONFLICT,"La cancelación requiere al menos 12 horas de anticipación");
        }
        reserva.estado=estado;
        return respuesta(reservas.saveAndFlush(reserva));
    }

    private List<Reserva> solapadas(LocalDateTime inicio,LocalDateTime fin,Long excluir) {
        return reservas.findByEstadoAndInicioLessThanAndFinGreaterThan(EstadoReserva.CONFIRMADA,fin,inicio)
            .stream().filter(r->excluir==null||!excluir.equals(r.id)).toList();
    }
    private List<Box> boxesLibres(List<Reserva> ocupadas) {
        Set<Long> ocupados=new HashSet<>();
        ocupadas.stream().filter(r->r.box!=null).forEach(r->ocupados.add(r.box.getId()));
        return boxes.findByActivoTrueOrderByIdAsc().stream().filter(b->!ocupados.contains(b.getId())).toList();
    }
    private void validarModalidad(CatalogoClient.Atencion atencion,Modalidad modalidad) {
        if((modalidad==Modalidad.ONLINE&&(!atencion.servicio().permiteOnline()||!atencion.especialista().permiteOnline()))
                ||(modalidad==Modalidad.PRESENCIAL&&!atencion.especialista().permitePresencial()))
            throw error(HttpStatus.BAD_REQUEST,"La modalidad no está disponible para esta atención");
    }
    private Reserva buscar(Long id) {
        return reservas.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Reserva no encontrada"));
    }
    private ReservaResponse respuesta(Reserva reserva) { return ReservaResponse.desde(reserva,distancia.umbral()); }
}
