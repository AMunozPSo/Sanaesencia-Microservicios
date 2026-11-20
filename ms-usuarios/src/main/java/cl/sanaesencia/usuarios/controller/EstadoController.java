package cl.sanaesencia.usuarios.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class EstadoController {

    @GetMapping("/api/usuarios/estado")
    public Map<String, String> obtenerEstado() {
        return Map.of(
                "servicio", "ms-usuarios",
                "estado", "Funcionando correctamente"
        );
    }
}
