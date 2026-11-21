package cl.sanaesencia.reservas.controller;
import cl.sanaesencia.reservas.dto.*;
import cl.sanaesencia.reservas.service.AgendaService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/agenda")
public class AgendaController {
    private final AgendaService agenda;
    public AgendaController(AgendaService agenda) { this.agenda=agenda; }
    @GetMapping("/{especialistaId}") public List<AgendaRequest.Franja> consultar(
            @PathVariable("especialistaId") Long id) { return agenda.consultar(id); }
    @PutMapping("/{especialistaId}") public List<AgendaRequest.Franja> guardar(
            @PathVariable("especialistaId") Long id,@Valid @RequestBody AgendaRequest solicitud,
            @AuthenticationPrincipal Cuenta cuenta) { return agenda.guardar(id,solicitud,cuenta); }
}
