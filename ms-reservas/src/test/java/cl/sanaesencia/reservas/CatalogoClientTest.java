package cl.sanaesencia.reservas;
import cl.sanaesencia.reservas.client.CatalogoClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class CatalogoClientTest {
    HttpServer server;
    CatalogoClient cliente;
    AtomicInteger estado=new AtomicInteger(200);
    AtomicReference<String> json=new AtomicReference<>();
    @BeforeEach void preparar() throws Exception {
        json.set("""
                {"id":1,"nombre":"Profesional","activo":true,"permitePresencial":true,"permiteOnline":true,
                 "servicios":[{"id":100,"nombre":"Terapia","precio":30000,"duracionMinutos":60,
                 "permiteOnline":true,"activo":true}]}
                """);
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/especialistas/1",exchange-> {
            byte[] bytes=json.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(estado.get(),bytes.length);
            exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.start();
        cliente=new CatalogoClient("http://127.0.0.1:"+server.getAddress().getPort());
    }
    @AfterEach void terminar() { server.stop(0); }
    @Test void leePerfilYDuracionDelServicio() {
        var atencion=cliente.atencion(1L,100L);
        assertEquals(60,atencion.servicio().duracionMinutos());
        assertEquals("Profesional",atencion.especialista().nombre());
    }
    @Test void rechazaServicioNoAsignado() { assertEquals(400,error(()->cliente.atencion(1L,999L))); }
    @Test void perfilInexistenteNoSeConfundeConCaida() {
        estado.set(404);assertEquals(404,error(()->cliente.perfil(1L)));
        estado.set(500);assertEquals(503,error(()->cliente.perfil(1L)));
    }
    @Test void perfilPausadoYServicioDesactivadoNoSonReservables() {
        String original=json.get();
        json.set(original.replace("\"activo\":true","\"activo\":false"));
        assertEquals(409,error(()->cliente.atencion(1L,100L)));
        json.set(original.replace("\"permiteOnline\":true,\"activo\":true","\"permiteOnline\":true,\"activo\":false"));
        assertEquals(409,error(()->cliente.atencion(1L,100L)));
    }
    @Test void respuestaInvalidaDevuelve503() {
        json.set("no-json");assertEquals(503,error(()->cliente.perfil(1L)));
    }
    private int error(Runnable accion) {
        return assertThrows(ResponseStatusException.class,accion::run).getStatusCode().value();
    }
}
