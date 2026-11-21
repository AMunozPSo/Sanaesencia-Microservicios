package cl.sanaesencia.reservas.dto;

import java.util.Map;

public record ErrorResponse(
        int estado,
        String mensaje,
        Map<String, String> errores
) {
}
