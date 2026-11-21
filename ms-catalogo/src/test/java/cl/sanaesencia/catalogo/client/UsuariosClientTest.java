package cl.sanaesencia.catalogo.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class UsuariosClientTest {
    HttpServer servidor;
    UsuariosClient cliente;
    AtomicInteger estado = new AtomicInteger(200);
    AtomicReference<String> respuesta = new AtomicReference<>();
    AtomicReference<String> recibido = new AtomicReference<>();
    AtomicReference<String> metodo = new AtomicReference<>();

    @BeforeEach void preparar() throws Exception {
        respuesta.set("""
                {"id":1,"correo":"admin@example.com","rol":"ADMIN","activo":true,"nombre":"Admin"}
                """);
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/usuarios/login", intercambio -> {
            metodo.set(intercambio.getRequestMethod());
            recibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] cuerpo = respuesta.get().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado.get(), cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidor.start();
        cliente = new UsuariosClient("http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @AfterEach void cerrar() { servidor.stop(0); }

    @Test void enviaLoginJsonYLeeCuentaSinCopiarCamposExtra() {
        var usuario = cliente.validarCredenciales("admin@example.com", "clave-test");
        assertEquals("POST", metodo.get());
        assertTrue(recibido.get().contains("\"correo\":\"admin@example.com\""));
        assertTrue(recibido.get().contains("\"password\":\"clave-test\""));
        assertEquals(1L, usuario.id());
        assertEquals("ADMIN", usuario.rol());
        assertTrue(usuario.activo());
    }

    @ParameterizedTest @ValueSource(ints = {400, 401})
    void rechazaCredencialesInvalidas(int codigo) {
        estado.set(codigo);
        respuesta.set("{\"mensaje\":\"Datos inválidos\"}");
        assertThrows(BadCredentialsException.class,
                () -> cliente.validarCredenciales("admin@example.com", "clave-test"));
    }

    @ParameterizedTest @ValueSource(ints = {403, 404, 500, 503})
    void otrosErroresDelProveedorSonFalloDelServicio(int codigo) {
        estado.set(codigo);
        assertThrows(AuthenticationServiceException.class,
                () -> cliente.validarCredenciales("admin@example.com", "clave-test"));
    }

    @ParameterizedTest @ValueSource(strings = {
            "{\"id\":1,\"correo\":\"a@example.com\",\"rol\":\"ADMIN\",\"activo\":false}",
            "{\"id\":1,\"correo\":\"a@example.com\",\"activo\":true}"
    })
    void rechazaCuentasInactivasOIncompletas(String json) {
        respuesta.set(json);
        assertThrows(BadCredentialsException.class,
                () -> cliente.validarCredenciales("admin@example.com", "clave-test"));
    }

    @ParameterizedTest @ValueSource(strings = {"no-es-json", "{}"})
    void respuestaIninterpretableEsFalloDelProveedor(String cuerpo) {
        respuesta.set(cuerpo);
        assertThrows(AuthenticationServiceException.class,
                () -> cliente.validarCredenciales("admin@example.com", "clave-test"));
    }
}
