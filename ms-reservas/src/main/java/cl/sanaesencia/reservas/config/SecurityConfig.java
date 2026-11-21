package cl.sanaesencia.reservas.config;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain seguridad(HttpSecurity http, UsuariosAuthenticationProvider proveedor) throws Exception {
        http.authenticationProvider(proveedor)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/estado").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated().anyRequest().denyAll())
            .httpBasic(b -> b.authenticationEntryPoint((req, res, error) -> {
                if (error instanceof AuthenticationServiceException)
                    ErroresSeguridad.escribir(res, 503, "El servicio de usuarios no está disponible");
                else {
                    res.setHeader("WWW-Authenticate", "Basic realm=\"SanaEsencia\"");
                    ErroresSeguridad.escribir(res, 401, "Credenciales ausentes o incorrectas");
                }
            }))
            .exceptionHandling(e -> e.accessDeniedHandler((req, res, error) ->
                ErroresSeguridad.escribir(res, 403, "No tienes permiso para esta operación")));
        return http.build();
    }
}
