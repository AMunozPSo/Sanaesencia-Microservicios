package cl.sanaesencia.catalogo.client;

import cl.sanaesencia.catalogo.dto.UsuarioAutenticadoResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class UsuariosClient {

    private final RestClient cliente;

    public UsuariosClient(
            @Value("${usuarios.api.url}") String url
    ) {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        cliente = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    public UsuarioAutenticadoResponse validarCredenciales(
            String correo,
            String password
    ) {
        UsuarioAutenticadoResponse usuario;

        try {
            usuario = cliente.post()
                    .uri("/api/usuarios/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "correo", correo,
                            "password", password
                    ))
                    .retrieve()
                    .body(UsuarioAutenticadoResponse.class);

        } catch (RestClientResponseException error) {

            int estado = error.getStatusCode().value();

            if (estado == 400 || estado == 401) {
                throw new BadCredentialsException(
                        "Correo o contraseña incorrectos"
                );
            }

            throw new AuthenticationServiceException(
                    "No se pudo verificar la cuenta"
            );

        } catch (RestClientException error) {
            throw new AuthenticationServiceException(
                    "El servicio de usuarios no está disponible"
            );
        }

        if (usuario == null
                || usuario.id() == null
                || usuario.correo() == null
                || usuario.rol() == null
                || !usuario.activo()) {
            throw new BadCredentialsException(
                    "Correo o contraseña incorrectos"
            );
        }

        return usuario;
    }
}
