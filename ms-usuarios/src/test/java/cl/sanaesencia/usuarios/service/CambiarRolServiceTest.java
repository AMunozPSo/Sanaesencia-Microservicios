package cl.sanaesencia.usuarios.service;

import cl.sanaesencia.usuarios.dto.CambiarRolRequest;
import cl.sanaesencia.usuarios.dto.UsuarioResponse;
import cl.sanaesencia.usuarios.model.Rol;
import cl.sanaesencia.usuarios.model.Usuario;
import cl.sanaesencia.usuarios.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CambiarRolServiceTest {

    private UsuarioRepository repositorio;
    private UsuarioService servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);

        servicio = new UsuarioService(
                repositorio,
                new BCryptPasswordEncoder()
        );

        usuario = new Usuario(
                "Ana",
                "Perez",
                "12345678-5",
                "paciente.prueba@example.com",
                "hash-de-prueba"
        );

        usuario.setId(1L);

        when(repositorio.findById(1L))
                .thenReturn(Optional.of(usuario));

        when(repositorio.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void conviertePacienteEnEspecialista() {
        UsuarioResponse respuesta = servicio.cambiarRol(
                1L,
                new CambiarRolRequest(Rol.ESPECIALISTA, 10L)
        );

        assertEquals(Rol.ESPECIALISTA, respuesta.rol());
        assertEquals(10L, respuesta.especialistaId());

        verify(repositorio).save(usuario);
    }

    @Test
    void convierteEspecialistaEnPacienteYQuitaVinculo() {
        usuario.setRol(Rol.ESPECIALISTA);
        usuario.setEspecialistaId(10L);

        UsuarioResponse respuesta = servicio.cambiarRol(
                1L,
                new CambiarRolRequest(Rol.PACIENTE, null)
        );

        assertEquals(Rol.PACIENTE, respuesta.rol());
        assertNull(respuesta.especialistaId());

        verify(repositorio).save(usuario);
    }

    @Test
    void rechazaEspecialistaSinPerfil() {
        comprobarRechazo(
                new CambiarRolRequest(Rol.ESPECIALISTA, null),
                HttpStatus.BAD_REQUEST
        );

        assertEquals(Rol.PACIENTE, usuario.getRol());
    }

    @Test
    void rechazaPacienteConPerfilDeEspecialista() {
        comprobarRechazo(
                new CambiarRolRequest(Rol.PACIENTE, 10L),
                HttpStatus.BAD_REQUEST
        );
    }

    @Test
    void impideAsignarRolAdminOModificarUnAdmin() {
        comprobarRechazo(
                new CambiarRolRequest(Rol.ADMIN, null),
                HttpStatus.BAD_REQUEST
        );

        usuario.setRol(Rol.ADMIN);

        comprobarRechazo(
                new CambiarRolRequest(Rol.PACIENTE, null),
                HttpStatus.BAD_REQUEST
        );

        assertEquals(Rol.ADMIN, usuario.getRol());
    }

    @Test
    void devuelve404SiLaCuentaNoExiste() {
        when(repositorio.findById(1L))
                .thenReturn(Optional.empty());

        comprobarRechazo(
                new CambiarRolRequest(Rol.PACIENTE, null),
                HttpStatus.NOT_FOUND
        );
    }

    private void comprobarRechazo(
            CambiarRolRequest solicitud,
            HttpStatus estadoEsperado
    ) {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.cambiarRol(1L, solicitud)
        );

        assertEquals(estadoEsperado, error.getStatusCode());

        verify(repositorio, never()).save(any(Usuario.class));
    }
}
