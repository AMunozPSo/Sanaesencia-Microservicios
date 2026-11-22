package cl.sanaesencia.reservas.service;
import cl.sanaesencia.reservas.dto.BoxRequest;
import cl.sanaesencia.reservas.model.*;
import cl.sanaesencia.reservas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.List;
import static cl.sanaesencia.reservas.service.ReglasAgenda.error;
@Service
public class BoxService {
    public record Respuesta(Long id,String nombre,String descripcion,boolean activo) {
        static Respuesta desde(Box b) { return new Respuesta(b.getId(),b.getNombre(),b.getDescripcion(),b.isActivo()); }
    }
    private final BoxRepository boxes;
    private final ReservaRepository reservas;
    private final ReglasAgenda reglas;
    private final Clock reloj;
    public BoxService(BoxRepository boxes,ReservaRepository reservas,ReglasAgenda reglas,Clock reloj) {
        this.boxes=boxes; this.reservas=reservas; this.reglas=reglas; this.reloj=reloj;
    }
    @Transactional(readOnly=true)
    public List<Respuesta> listar(boolean todos) {
        return (todos?boxes.findAll():boxes.findByActivoTrueOrderByIdAsc()).stream().map(Respuesta::desde).toList();
    }
    @Transactional
    public Respuesta guardar(Long id,BoxRequest solicitud) {
        reglas.bloquear();
        Box box=id==null?new Box():buscar(id);
        String nombre=solicitud.nombre().strip();
        if(boxes.existsByNombreIgnoreCaseAndIdNot(nombre,id==null?-1L:id))
            throw error(HttpStatus.CONFLICT,"Ya existe un box con ese nombre");
        box.nombre=nombre; box.descripcion=solicitud.descripcion().strip();
        return Respuesta.desde(boxes.saveAndFlush(box));
    }
    @Transactional
    public Respuesta estado(Long id,boolean activo) {
        reglas.bloquear(); Box box=buscar(id);
        if(!activo&&reservas.existsByBox_IdAndEstadoAndFinAfter(id,EstadoReserva.CONFIRMADA,LocalDateTime.now(reloj)))
            throw error(HttpStatus.CONFLICT,"El box tiene reservas pendientes y no puede desactivarse");
        box.activo=activo;
        return Respuesta.desde(boxes.saveAndFlush(box));
    }
    private Box buscar(Long id) {
        return boxes.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Box no encontrado"));
    }
}
