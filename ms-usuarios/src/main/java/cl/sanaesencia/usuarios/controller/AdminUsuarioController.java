package cl.sanaesencia.usuarios.controller;

import cl.sanaesencia.usuarios.dto.CambiarRolRequest;
import cl.sanaesencia.usuarios.dto.UsuarioResponse;
import cl.sanaesencia.usuarios.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(
            UsuarioService usuarioService
    ) {
        this.usuarioService = usuarioService;
    }

    @PatchMapping("/{id}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(
            @PathVariable("id") Long id,
            @Valid @RequestBody CambiarRolRequest solicitud
    ) {
        return ResponseEntity.ok(
                usuarioService.cambiarRol(id, solicitud)
        );
    }
}
