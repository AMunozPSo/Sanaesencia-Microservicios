package cl.sanaesencia.reservas.controller;
import cl.sanaesencia.reservas.dto.*;
import cl.sanaesencia.reservas.model.*;
import cl.sanaesencia.reservas.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.List;
@RestController
@RequestMapping("/api")
public class ReservasController {
    private final ReservaService reservas;
    private final Clock reloj;
    public ReservasController(ReservaService reservas,Clock reloj) { this.reservas=reservas; this.reloj=reloj; }

    @GetMapping("/estado") public java.util.Map<String,String> estado() {
        return java.util.Map.of("servicio","ms-reservas","estado","Funcionando correctamente");
    }
    @PostMapping("/reservas") public ResponseEntity<ReservaResponse> crear(
            @Valid @RequestBody ReservaRequest solicitud,@AuthenticationPrincipal Cuenta cuenta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservas.crear(solicitud,cuenta));
    }
    @GetMapping("/reservas") public List<ReservaResponse> listar(
            @RequestParam(name="mes",required=false) String mes,@AuthenticationPrincipal Cuenta cuenta) {
        YearMonth periodo;
        try { periodo=mes==null?YearMonth.now(reloj):YearMonth.parse(mes); }
        catch(java.time.format.DateTimeParseException e) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Usa el formato yyyy-MM para el mes");
        }
        return reservas.listar(periodo,cuenta);
    }
    @GetMapping("/reservas/{id}") public ReservaResponse consultar(@PathVariable("id") Long id,
            @AuthenticationPrincipal Cuenta cuenta) { return reservas.consultar(id,cuenta); }
    @PutMapping("/reservas/{id}") public ReservaResponse reprogramar(@PathVariable("id") Long id,
            @Valid @RequestBody ReservaRequest solicitud,@AuthenticationPrincipal Cuenta cuenta) {
        return reservas.reprogramar(id,solicitud,cuenta);
    }
    @PatchMapping("/reservas/{id}/estado") public ReservaResponse estado(@PathVariable("id") Long id,
            @Valid @RequestBody EstadoReservaRequest solicitud,@AuthenticationPrincipal Cuenta cuenta) {
        return reservas.cambiarEstado(id,solicitud.estado(),cuenta);
    }
    @DeleteMapping("/reservas/{id}") public ResponseEntity<Void> cancelar(@PathVariable("id") Long id,
            @AuthenticationPrincipal Cuenta cuenta) {
        reservas.cambiarEstado(id,EstadoReserva.CANCELADA,cuenta); return ResponseEntity.noContent().build();
    }
    @GetMapping("/disponibilidad") public List<ReservaService.Turno> disponibilidad(
            @RequestParam("especialistaId") Long especialistaId,@RequestParam("servicioId") Long servicioId,
            @RequestParam("fecha") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam("modalidad") Modalidad modalidad,@AuthenticationPrincipal Cuenta cuenta) {
        return reservas.disponibilidad(especialistaId,servicioId,fecha,modalidad,cuenta);
    }
    @GetMapping("/ubicacion/sugerencia") public ReservaService.Sugerencia sugerir(
            @RequestParam("especialistaId") Long especialistaId,@RequestParam("servicioId") Long servicioId,
            @RequestParam(name="latitud",required=false) Double latitud,
            @RequestParam(name="longitud",required=false) Double longitud) {
        return reservas.sugerir(especialistaId,servicioId,latitud,longitud);
    }
}
