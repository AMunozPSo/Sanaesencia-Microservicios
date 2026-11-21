package cl.sanaesencia.reservas.controller;
import cl.sanaesencia.reservas.dto.*;
import cl.sanaesencia.reservas.service.BoxService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
public class BoxController {
    private final BoxService boxes;
    public BoxController(BoxService boxes) { this.boxes=boxes; }
    @GetMapping("/api/boxes") public List<BoxService.Respuesta> listar() { return boxes.listar(false); }
    @GetMapping("/api/admin/boxes") public List<BoxService.Respuesta> todos() { return boxes.listar(true); }
    @PostMapping("/api/admin/boxes") public ResponseEntity<BoxService.Respuesta> crear(@Valid @RequestBody BoxRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boxes.guardar(null,solicitud));
    }
    @PutMapping("/api/admin/boxes/{id}") public BoxService.Respuesta actualizar(@PathVariable("id") Long id,
            @Valid @RequestBody BoxRequest solicitud) { return boxes.guardar(id,solicitud); }
    @PatchMapping("/api/admin/boxes/{id}/estado") public BoxService.Respuesta estado(@PathVariable("id") Long id,
            @Valid @RequestBody EstadoRequest solicitud) { return boxes.estado(id,solicitud.activo()); }
}
