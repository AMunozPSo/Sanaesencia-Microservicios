package cl.sanaesencia.usuarios.controller;

import cl.sanaesencia.usuarios.dto.RegistroUsuarioRequest;
import cl.sanaesencia.usuarios.dto.UsuarioResponse;
import cl.sanaesencia.usuarios.service.UsuarioService;
import cl.sanaesencia.usuarios.dto.ActualizarUsuarioRequest;
import org.springframework.web.bind.annotation.PutMapping;
import cl.sanaesencia.usuarios.dto.LoginRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(
            @Valid @RequestBody RegistroUsuarioRequest solicitud
    ) {
        UsuarioResponse respuesta =
                usuarioService.registrar(solicitud);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(
                usuarioService.obtenerPorId(id)
        );
    }
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody ActualizarUsuarioRequest solicitud
    ) {
        return ResponseEntity.ok(
                usuarioService.actualizar(id, solicitud)
        );
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @PathVariable("id") Long id
    ) {
        usuarioService.desactivar(id);

        return ResponseEntity.noContent().build();
    }
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> iniciarSesion(
            @Valid @RequestBody LoginRequest solicitud
    ) {
        return ResponseEntity.ok(
                usuarioService.iniciarSesion(solicitud)
        );
    }
}