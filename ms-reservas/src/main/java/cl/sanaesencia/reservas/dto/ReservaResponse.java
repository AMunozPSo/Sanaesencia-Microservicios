package cl.sanaesencia.reservas.dto;
import cl.sanaesencia.reservas.model.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
public record ReservaResponse(Long id, Long pacienteId, String pacienteNombre, Long especialistaId,
        String especialistaNombre, Long servicioId, String servicioNombre, BigDecimal precio,
        LocalDateTime inicio, LocalDateTime fin, Modalidad modalidad, EstadoReserva estado,
        Long boxId, String boxNombre, Double distanciaKm, boolean sugiereOnline) {
    public static ReservaResponse desde(Reserva r, double umbral) {
        return new ReservaResponse(r.id,r.pacienteId,r.pacienteNombre,r.especialistaId,r.especialistaNombre,
            r.servicioId,r.servicioNombre,r.precio,r.inicio,r.fin,r.modalidad,r.estado,
            r.box==null?null:r.box.id,r.box==null?null:r.box.nombre,r.distanciaKm,
            r.onlineDisponible && r.distanciaKm!=null && r.distanciaKm>umbral);
    }
}
