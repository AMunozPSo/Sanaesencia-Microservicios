package cl.sanaesencia.catalogo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioAutenticadoResponse(
        Long id,
        String correo,
        String rol,
        boolean activo
) {
}
