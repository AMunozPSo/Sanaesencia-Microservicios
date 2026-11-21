package cl.sanaesencia.reservas.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

final class ErroresSeguridad {
    private ErroresSeguridad() {}

    static void escribir(HttpServletResponse respuesta, int estado, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.setContentType("application/json");
        // Solo recibe mensajes constantes internos, nunca datos ni credenciales del cliente.
        respuesta.getWriter().write("{\"estado\":" + estado + ",\"mensaje\":\""
                + mensaje + "\",\"errores\":{}}");
    }
}
