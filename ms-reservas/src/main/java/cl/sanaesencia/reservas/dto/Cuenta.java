package cl.sanaesencia.reservas.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.security.Principal;
@JsonIgnoreProperties(ignoreUnknown = true)
public record Cuenta(Long id, String correo, String nombre, String apellido, String rol,
                     Long especialistaId, boolean activo) implements Principal {
    @Override public String getName() { return correo; }
    public boolean esAdmin() { return "ADMIN".equals(rol); }
    public boolean esPaciente() { return "PACIENTE".equals(rol); }
    public boolean esEspecialista() { return "ESPECIALISTA".equals(rol); }
    public String nombreCompleto() {
        return ((nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido)).strip();
    }
}
