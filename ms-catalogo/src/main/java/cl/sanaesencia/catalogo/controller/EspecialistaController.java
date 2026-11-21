package cl.sanaesencia.catalogo.controller;

import cl.sanaesencia.catalogo.dto.*;
import cl.sanaesencia.catalogo.service.EspecialistaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/especialistas")
public class EspecialistaController {
    private final EspecialistaService servicio;
    public EspecialistaController(EspecialistaService servicio) { this.servicio = servicio; }

    @GetMapping
    public List<EspecialistaResponse> listar(@RequestParam(name = "servicioId", required = false) Long servicioId) {
        return servicio.listarActivos(servicioId);
    }

    @GetMapping("/{id}")
    public EspecialistaResponse obtener(@PathVariable("id") Long id) {
        return servicio.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<EspecialistaResponse> registrar(@Valid @RequestBody EspecialistaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
    }

    @PutMapping("/{id}")
    public EspecialistaResponse actualizar(@PathVariable("id") Long id,
                                          @Valid @RequestBody EspecialistaRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/estado")
    public EspecialistaResponse cambiarEstado(@PathVariable("id") Long id,
                                             @Valid @RequestBody EstadoRequest solicitud) {
        return servicio.cambiarEstado(id, solicitud.activo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable("id") Long id) {
        servicio.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
