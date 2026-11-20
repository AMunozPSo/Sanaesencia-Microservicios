package cl.sanaesencia.usuarios.service;

import cl.sanaesencia.usuarios.dto.LoginRequest;
import cl.sanaesencia.usuarios.dto.UsuarioResponse;
import cl.sanaesencia.usuarios.model.Usuario;
import cl.sanaesencia.usuarios.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginServiceTest {

    private UsuarioRepository repositorio;
    private UsuarioService servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);

        PasswordEncoder encoder = new BCryptPasswordEncoder();

        servicio = new UsuarioService(repositorio, encoder);

        usuario = new Usuario(
                "Ana",
                "Perez",
                "12345678-5",
                "paciente.prueba@example.com",
                encoder.encode("PruebaSana2026!")
        );

        usuario.setId(1L);
    }

    @Test
    void permiteLoginConCredencialesCorrectas() {
        when(repositorio.findByCorreoIgnoreCase(
                "paciente.prueba@example.com"
        )).thenReturn(Optional.of(usuario));

        UsuarioResponse respuesta = servicio.iniciarSesion(
                new LoginRequest(
                        "PACIENTE.PRUEBA@EXAMPLE.COM",
                        "PruebaSana2026!"
                )
        );

        assertEquals(1L, respuesta.id());
        assertEquals("Ana", respuesta.nombre());
        assertTrue(respuesta.activo());

        verify(repositorio).findByCorreoIgnoreCase(
                "paciente.prueba@example.com"
        );
    }

    @Test
    void rechazaPasswordIncorrecta() {
        when(repositorio.findByCorreoIgnoreCase(
                "paciente.prueba@example.com"
        )).thenReturn(Optional.of(usuario));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.iniciarSesion(
                        new LoginRequest(
                                "paciente.prueba@example.com",
                                "Incorrecta2026!"
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals(
                "Correo o contraseña incorrectos",
                error.getReason()
        );
    }

    @Test
    void rechazaUsuarioDesactivado() {
        usuario.setActivo(false);

        when(repositorio.findByCorreoIgnoreCase(
                "paciente.prueba@example.com"
        )).thenReturn(Optional.of(usuario));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.iniciarSesion(
                        new LoginRequest(
                                "paciente.prueba@example.com",
                                "PruebaSana2026!"
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals(
                "Correo o contraseña incorrectos",
                error.getReason()
        );
    }

    @Test
    void rechazaCorreoNoRegistrado() {
        when(repositorio.findByCorreoIgnoreCase(
                "noexiste@example.com"
        )).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.iniciarSesion(
                        new LoginRequest(
                                "noexiste@example.com",
                                "PruebaSana2026!"
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        assertEquals(
                "Correo o contraseña incorrectos",
                error.getReason()
        );
    }
}
