package cl.sanaesencia.usuarios.service;

import cl.sanaesencia.usuarios.repository.UsuarioRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("accesoUsuario")
public class AccesoUsuario {

    private final UsuarioRepository usuarioRepository;

    public AccesoUsuario(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public boolean esTitular(Long id, String correo) {
        return usuarioRepository.findById(id)
                .map(usuario ->
                        usuario.getCorreo()
                                .equalsIgnoreCase(correo)
                )
                .orElse(false);
    }
}
