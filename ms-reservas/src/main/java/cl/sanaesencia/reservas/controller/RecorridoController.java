package cl.sanaesencia.reservas.controller;

import cl.sanaesencia.reservas.client.TrueWayClient;
import cl.sanaesencia.reservas.dto.Cuenta;
import cl.sanaesencia.reservas.service.LimiteRecorridos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rutas")
public class RecorridoController {
    public record Origen(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
                         @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud) {}
    private final TrueWayClient proveedor;
    private final LimiteRecorridos limite;
    private final double latitud,longitud;
    public RecorridoController(TrueWayClient proveedor,LimiteRecorridos limite,
            @Value("${centro.latitud}") double latitud,@Value("${centro.longitud}") double longitud) {
        this.proveedor=proveedor;this.limite=limite;this.latitud=latitud;this.longitud=longitud;
    }
    @PostMapping("/sede")
    public ResponseEntity<TrueWayClient.Respuesta> consultar(@Valid @RequestBody Origen origen,
            @AuthenticationPrincipal Cuenta cuenta) {
        if(!Double.isFinite(origen.latitud())||!Double.isFinite(origen.longitud()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La ubicación no es válida");
        limite.consumir(cuenta.id());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(proveedor.consultar(origen.latitud(),origen.longitud(),latitud,longitud));
    }
}
