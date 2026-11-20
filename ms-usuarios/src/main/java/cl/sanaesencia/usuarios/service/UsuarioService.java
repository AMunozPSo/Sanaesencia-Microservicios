package cl.sanaesencia.usuarios.service;

import cl.sanaesencia.usuarios.dto.RegistroUsuarioRequest;
import cl.sanaesencia.usuarios.dto.UsuarioResponse;
import cl.sanaesencia.usuarios.model.Usuario;
import cl.sanaesencia.usuarios.repository.UsuarioRepository;
import cl.sanaesencia.usuarios.validation.ValidadorRun;
import cl.sanaesencia.usuarios.dto.ActualizarUsuarioRequest;
import cl.sanaesencia.usuarios.dto.LoginRequest;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import cl.sanaesencia.usuarios.dto.CambiarRolRequest;
import cl.sanaesencia.usuarios.model.Rol;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@Validated
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String hashAuxiliarLogin;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;

        this.hashAuxiliarLogin = passwordEncoder.encode(
                UUID.randomUUID().toString()
        );
    }

    @Transactional
    public UsuarioResponse registrar(
            @NotNull @Valid RegistroUsuarioRequest solicitud
    ) {
        String correo = solicitud.correo()
                .strip()
                .toLowerCase(Locale.ROOT);

        String run = solicitud.run()
                .toUpperCase(Locale.ROOT);

        if (!ValidadorRun.esValido(run) || run.startsWith("0")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El RUN debe ser válido y no llevar ceros iniciales"
            );
        }

        if (solicitud.password()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña es demasiado larga. Usa menos caracteres"
            );
        }

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya está registrado"
            );
        }

        if (usuarioRepository.existsByRun(run)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El RUN ya está registrado"
            );
        }

        String passwordHash =
                passwordEncoder.encode(solicitud.password());

        Usuario usuario = new Usuario(
                solicitud.nombre().strip(),
                solicitud.apellido().strip(),
                run,
                correo,
                passwordHash
        );

        try {
            Usuario guardado =
                    usuarioRepository.saveAndFlush(usuario);

            return UsuarioResponse.desde(guardado);

        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo registrar: el correo o RUN ya está registrado",
                    error
            );
        }
    }
    @PreAuthorize("hasRole('ADMIN') or @accesoUsuario.esTitular(#p0, authentication.name)")
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        return UsuarioResponse.desde(usuario);
    }
    @PreAuthorize("hasRole('ADMIN') or @accesoUsuario.esTitular(#p0, authentication.name)")
    @Transactional
    public UsuarioResponse actualizar(
            Long id,
            @NotNull @Valid ActualizarUsuarioRequest solicitud
    ) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        usuario.setNombre(solicitud.nombre().strip());
        usuario.setApellido(solicitud.apellido().strip());

        Usuario guardado = usuarioRepository.save(usuario);

        return UsuarioResponse.desde(guardado);
    }
    @PreAuthorize("hasRole('ADMIN') or @accesoUsuario.esTitular(#p0, authentication.name)")
    @Transactional
    public void desactivar(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        usuario.setActivo(false);

        usuarioRepository.save(usuario);
    }
    @Transactional(readOnly = true)
    public UsuarioResponse iniciarSesion(
            @NotNull @Valid LoginRequest solicitud
    ) {
        String correo = solicitud.correo()
                .strip()
                .toLowerCase(Locale.ROOT);

        if (solicitud.password()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contraseña incorrectos"
            );
        }

        Usuario usuario = usuarioRepository
                .findByCorreoIgnoreCase(correo)
                .orElse(null);

        String hash = usuario == null
                ? hashAuxiliarLogin
                : usuario.getPasswordHash();

        boolean passwordCorrecta = passwordEncoder.matches(
                solicitud.password(),
                hash
        );

        if (usuario == null
                || !passwordCorrecta
                || !usuario.isActivo()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contraseña incorrectos"
            );
        }

        return UsuarioResponse.desde(usuario);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UsuarioResponse cambiarRol(
            Long id,
            @NotNull @Valid CambiarRolRequest solicitud
    ) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        if (usuario.getRol() == Rol.ADMIN
                || solicitud.rol() == Rol.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Este endpoint solo permite gestionar pacientes y especialistas"
            );
        }

        if (solicitud.rol() == Rol.ESPECIALISTA) {

            if (solicitud.especialistaId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Debes indicar el ID del perfil del especialista"
                );
            }

            usuario.setEspecialistaId(
                    solicitud.especialistaId()
            );

        } else {

            if (solicitud.especialistaId() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Un paciente no debe tener un ID de especialista"
                );
            }

            usuario.setEspecialistaId(null);
        }

        usuario.setRol(solicitud.rol());

        Usuario guardado = usuarioRepository.save(usuario);

        return UsuarioResponse.desde(guardado);
    }
}
