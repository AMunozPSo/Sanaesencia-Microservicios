package cl.sanaesencia.catalogo.controller;

import cl.sanaesencia.catalogo.dto.ServicioTerapiaRequest;
import cl.sanaesencia.catalogo.dto.ServicioTerapiaResponse;
import cl.sanaesencia.catalogo.dto.EstadoRequest;
import cl.sanaesencia.catalogo.service.ServicioTerapiaService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServicioTerapiaController {

    private final ServicioTerapiaService servicio;

    public ServicioTerapiaController(
            ServicioTerapiaService servicio
    ) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ServicioTerapiaResponse> registrar(
            @Valid @RequestBody ServicioTerapiaRequest solicitud
    ) {
        ServicioTerapiaResponse respuesta =
                servicio.registrar(solicitud);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<ServicioTerapiaResponse>>
    listarActivos() {
        return ResponseEntity.ok(
                servicio.listarActivos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioTerapiaResponse> obtenerPorId(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(
                servicio.obtenerPorId(id)
        );
    }
    @PutMapping("/{id}")
    public ResponseEntity<ServicioTerapiaResponse> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody ServicioTerapiaRequest solicitud
    ) {
        return ResponseEntity.ok(
                servicio.actualizar(id, solicitud)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @PathVariable("id") Long id
    ) {
        servicio.desactivar(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ServicioTerapiaResponse> cambiarEstado(
            @PathVariable("id") Long id,
            @Valid @RequestBody EstadoRequest solicitud
    ) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, solicitud.activo()));
    }
}
