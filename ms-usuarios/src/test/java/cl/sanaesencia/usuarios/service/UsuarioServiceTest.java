package cl.sanaesencia.usuarios.service;

import cl.sanaesencia.usuarios.config.PasswordConfig;
import cl.sanaesencia.usuarios.dto.RegistroUsuarioRequest;
import cl.sanaesencia.usuarios.dto.UsuarioResponse;
import cl.sanaesencia.usuarios.model.Rol;
import cl.sanaesencia.usuarios.model.Usuario;
import cl.sanaesencia.usuarios.repository.UsuarioRepository;
import cl.sanaesencia.usuarios.dto.ActualizarUsuarioRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private UsuarioService usuarioService;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = new PasswordConfig().passwordEncoder();

        usuarioService = new UsuarioService(
                usuarioRepository,
                passwordEncoder
        );
    }

    @Test
    void registraPacienteConPasswordProtegida() {

        RegistroUsuarioRequest solicitud = new RegistroUsuarioRequest(
                " Ana ",
                " Perez ",
                "1000005-k",
                "PACIENTE.PRUEBA@EXAMPLE.COM",
                "PruebaSana2026!"
        );

        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenAnswer(invocacion -> {
                    Usuario usuario = invocacion.getArgument(0);
                    usuario.setId(1L);
                    return usuario;
                });

        UsuarioResponse respuesta =
                usuarioService.registrar(solicitud);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).saveAndFlush(captor.capture());

        Usuario guardado = captor.getValue();

        assertEquals("Ana", guardado.getNombre());
        assertEquals("Perez", guardado.getApellido());
        assertEquals("1000005-K", guardado.getRun());

        assertEquals(
                "paciente.prueba@example.com",
                guardado.getCorreo()
        );

        assertEquals(Rol.PACIENTE, guardado.getRol());
        assertNull(guardado.getEspecialistaId());
        assertTrue(guardado.isActivo());

        assertNotEquals(
                solicitud.password(),
                guardado.getPasswordHash()
        );

        assertTrue(passwordEncoder.matches(
                solicitud.password(),
                guardado.getPasswordHash()
        ));

        assertEquals(Long.valueOf(1L), respuesta.id());
        assertEquals(Rol.PACIENTE, respuesta.rol());
    }

    @Test
    void rechazaRunIncorrectoSinConsultarLaBaseDeDatos() {

        RegistroUsuarioRequest solicitud = new RegistroUsuarioRequest(
                "Ana",
                "Perez",
                "12345678-9",
                "paciente.prueba@example.com",
                "PruebaSana2026!"
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.registrar(solicitud)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        verifyNoInteractions(usuarioRepository);
    }
    @Test
    void rechazaCorreoDuplicado() {

        RegistroUsuarioRequest solicitud = new RegistroUsuarioRequest(
                "Ana",
                "Perez",
                "12345678-5",
                "paciente.prueba@example.com",
                "PruebaSana2026!"
        );

        when(usuarioRepository.existsByCorreoIgnoreCase(
                "paciente.prueba@example.com"
        )).thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.registrar(solicitud)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(usuarioRepository, never())
                .saveAndFlush(any(Usuario.class));
    }

    @Test
    void rechazaRunDuplicado() {

        RegistroUsuarioRequest solicitud = new RegistroUsuarioRequest(
                "Ana",
                "Perez",
                "12345678-5",
                "nuevo@example.com",
                "PruebaSana2026!"
        );

        when(usuarioRepository.existsByRun("12345678-5"))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.registrar(solicitud)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(usuarioRepository, never())
                .saveAndFlush(any(Usuario.class));
    }

    @Test
    void consultaUsuarioExistente() {

        Usuario usuario = new Usuario(
                "Ana",
                "Perez",
                "12345678-5",
                "paciente.prueba@example.com",
                "hash-de-prueba"
        );

        usuario.setId(1L);

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        UsuarioResponse respuesta = usuarioService.obtenerPorId(1L);

        assertEquals(Long.valueOf(1L), respuesta.id());
        assertEquals("Ana", respuesta.nombre());

        assertEquals(
                "paciente.prueba@example.com",
                respuesta.correo()
        );

        assertEquals(Rol.PACIENTE, respuesta.rol());
    }

    @Test
    void devuelve404CuandoElUsuarioNoExiste() {

        when(usuarioRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.obtenerPorId(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
    @Test
    void actualizaNombreYApellido() {

        Usuario usuario = crearUsuarioDePrueba();

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ActualizarUsuarioRequest solicitud =
                new ActualizarUsuarioRequest(
                        " Ana Maria ",
                        " Perez Soto "
                );

        UsuarioResponse respuesta =
                usuarioService.actualizar(1L, solicitud);

        assertEquals(Long.valueOf(1L), respuesta.id());
        assertEquals("Ana Maria", respuesta.nombre());
        assertEquals("Perez Soto", respuesta.apellido());

        assertEquals("12345678-5", respuesta.run());
        assertEquals(Rol.PACIENTE, respuesta.rol());
        assertEquals("hash-de-prueba", usuario.getPasswordHash());

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void actualizarUsuarioInexistenteDevuelve404() {

        when(usuarioRepository.findById(999L))
                .thenReturn(Optional.empty());

        ActualizarUsuarioRequest solicitud =
                new ActualizarUsuarioRequest("Ana", "Perez");

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.actualizar(999L, solicitud)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void desactivaUsuarioSinEliminarSuRegistro() {

        Usuario usuario = crearUsuarioDePrueba();

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        usuarioService.desactivar(1L);

        assertFalse(usuario.isActivo());
        assertEquals(Long.valueOf(1L), usuario.getId());

        verify(usuarioRepository).save(usuario);
        verify(usuarioRepository, never()).deleteById(anyLong());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    void desactivarUsuarioInexistenteDevuelve404() {

        when(usuarioRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.desactivar(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    private Usuario crearUsuarioDePrueba() {

        Usuario usuario = new Usuario(
                "Ana",
                "Perez",
                "12345678-5",
                "paciente.prueba@example.com",
                "hash-de-prueba"
        );

        usuario.setId(1L);

        return usuario;
    }
}
