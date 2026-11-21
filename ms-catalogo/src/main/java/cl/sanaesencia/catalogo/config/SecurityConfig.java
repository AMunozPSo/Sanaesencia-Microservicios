package cl.sanaesencia.catalogo.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UsuariosAuthenticationProvider proveedor
    ) throws Exception {

        http
                .authenticationProvider(proveedor)

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/servicios",
                                "/api/servicios/*",
                                "/api/especialistas",
                                "/api/especialistas/*"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/servicios",
                                "/api/especialistas"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/servicios/*",
                                "/api/especialistas/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/servicios/*/estado",
                                "/api/especialistas/*/estado"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/servicios/*",
                                "/api/especialistas/*"
                        ).hasRole("ADMIN")

                        .requestMatchers("/api/admin/catalogo/**").hasRole("ADMIN")
                        .anyRequest().denyAll()
                )

                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, error) -> {
                    if (error instanceof AuthenticationServiceException) {
                        ErroresSeguridad.escribir(response, 503, "El servicio de usuarios no está disponible");
                    } else {
                        response.setHeader("WWW-Authenticate", "Basic realm=\"SanaEsencia\"");
                        ErroresSeguridad.escribir(response, 401, "Credenciales ausentes o incorrectas");
                    }
                }))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, error) ->
                        ErroresSeguridad.escribir(response, 403, "No tienes permiso para esta operación")));

        return http.build();
    }
}
