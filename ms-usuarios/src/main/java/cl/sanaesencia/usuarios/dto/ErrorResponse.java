package cl.sanaesencia.usuarios.dto;

import java.util.Map;

public record ErrorResponse(
        int estado,
        String mensaje,
        Map<String, String> errores
) {
}
