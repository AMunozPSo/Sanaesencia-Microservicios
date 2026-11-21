package cl.sanaesencia.catalogo.config;

import cl.sanaesencia.catalogo.client.UsuariosClient;
import cl.sanaesencia.catalogo.dto.UsuarioAutenticadoResponse;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class UsuariosAuthenticationProvider
        implements AuthenticationProvider {

    private final UsuariosClient usuariosClient;

    public UsuariosAuthenticationProvider(
            UsuariosClient usuariosClient
    ) {
        this.usuariosClient = usuariosClient;
    }

    @Override
    public Authentication authenticate(
            Authentication authentication
    ) {
        if (authentication.getCredentials() == null) {
            throw new BadCredentialsException(
                    "Correo o contraseña incorrectos"
            );
        }

        String correo = authentication.getName()
                .strip()
                .toLowerCase(Locale.ROOT);

        String password = authentication.getCredentials()
                .toString();

        UsuarioAutenticadoResponse usuario =
                usuariosClient.validarCredenciales(
                        correo,
                        password
                );

        Set<String> rolesPermitidos = Set.of(
                "PACIENTE",
                "ESPECIALISTA",
                "ADMIN"
        );

        if (usuario == null || usuario.correo() == null || usuario.rol() == null || !usuario.activo()
                || !correo.equalsIgnoreCase(usuario.correo())
                || !rolesPermitidos.contains(usuario.rol())) {
            throw new BadCredentialsException(
                    "La cuenta no tiene una identidad o rol válido"
            );
        }

        UserDetails principal = User
                .withUsername(usuario.correo())
                .password("")
                .roles(usuario.rol())
                .build();

        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication);
    }
}
