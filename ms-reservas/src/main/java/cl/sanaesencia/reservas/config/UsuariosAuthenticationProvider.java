package cl.sanaesencia.reservas.config;
import cl.sanaesencia.reservas.client.UsuariosClient;
import cl.sanaesencia.reservas.dto.Cuenta;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;
import java.util.Set;
@Component
public class UsuariosAuthenticationProvider implements AuthenticationProvider {
    private final UsuariosClient usuarios;
    public UsuariosAuthenticationProvider(UsuariosClient usuarios) { this.usuarios = usuarios; }
    @Override public Authentication authenticate(Authentication solicitud) {
        if (solicitud.getCredentials() == null) throw new BadCredentialsException("Credenciales incorrectas");
        String correo = solicitud.getName().strip().toLowerCase(Locale.ROOT);
        Cuenta cuenta = usuarios.validarCredenciales(correo, solicitud.getCredentials().toString());
        if (cuenta == null || cuenta.id() == null || !cuenta.activo() || cuenta.rol() == null
                || !correo.equalsIgnoreCase(cuenta.correo())
                || !Set.of("ADMIN", "PACIENTE", "ESPECIALISTA").contains(cuenta.rol()))
            throw new BadCredentialsException("Credenciales incorrectas");
        return UsernamePasswordAuthenticationToken.authenticated(cuenta, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + cuenta.rol())));
    }
    @Override public boolean supports(Class<?> tipo) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
    }
}
