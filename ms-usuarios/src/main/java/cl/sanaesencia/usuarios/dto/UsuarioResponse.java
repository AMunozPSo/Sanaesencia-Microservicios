package cl.sanaesencia.usuarios.dto;

import cl.sanaesencia.usuarios.model.Rol;
import cl.sanaesencia.usuarios.model.Usuario;

public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String run,
        String correo,
        Rol rol,
        Long especialistaId,
        boolean activo
) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRun(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getEspecialistaId(),
                usuario.isActivo()
        );
    }
}
