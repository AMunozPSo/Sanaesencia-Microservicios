package cl.sanaesencia.reservas.client;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class TrueWayClientTest {
    HttpServer servidor;
    TrueWayClient cliente;
    AtomicInteger estado=new AtomicInteger(200),peticiones=new AtomicInteger();
    AtomicReference<String> cuerpo=new AtomicReference<>(),clave=new AtomicReference<>(),auth=new AtomicReference<>(),host=new AtomicReference<>(),consulta=new AtomicReference<>();
    @BeforeEach void iniciar() throws Exception {
        cuerpo.set("{\"route\":{\"distance\":1825,\"duration\":369,\"geometry\":{}}}");
        servidor=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        servidor.createContext("/FindDrivingRoute",e->{
            peticiones.incrementAndGet();clave.set(e.getRequestHeaders().getFirst("X-RapidAPI-Key"));
            auth.set(e.getRequestHeaders().getFirst("Authorization"));host.set(e.getRequestHeaders().getFirst("X-RapidAPI-Host"));
            consulta.set(e.getRequestURI().getQuery());byte[] bytes=cuerpo.get().getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(estado.get(),bytes.length);
            e.getResponseBody().write(bytes);e.close();
        });servidor.start();cliente=new TrueWayClient("secreto-solo-fixture","http://127.0.0.1:"+servidor.getAddress().getPort());
    }
    @AfterEach void terminar(){servidor.stop(0);}
    @Test void enviaClaveSoloAlProveedorYUsaElDestinoDelCentro() {
        var ruta=cliente.consultar(-31.9,-71.5,-31.909825,-71.512501);
        assertEquals(1825,ruta.route().distance());assertEquals(369,ruta.route().duration());
        assertEquals("secreto-solo-fixture",clave.get());assertEquals("trueway-directions2.p.rapidapi.com",host.get());
        assertNull(auth.get());assertEquals("stops=-31.9,-71.5;-31.909825,-71.512501",consulta.get());
    }
    @Test void sinClaveNoConsultaAlProveedor() {
        var vacio=new TrueWayClient(" ","http://127.0.0.1:"+servidor.getAddress().getPort());
        assertEquals(503,assertThrows(ResponseStatusException.class,()->vacio.consultar(0,0,0,0)).getStatusCode().value());
        assertEquals(0,peticiones.get());
    }
    @Test void erroresDelProveedorNoExponenRespuestaNiClave() {
        cuerpo.set("secreto-solo-fixture respuesta interna");
        for(int codigo:new int[]{401,403,429,500}) {
            estado.set(codigo);var error=assertThrows(ResponseStatusException.class,()->cliente.consultar(0,0,0,0));
            assertEquals(codigo==429?429:503,error.getStatusCode().value());
            assertFalse(error.getReason().contains("secreto"));assertFalse(error.getReason().contains("interna"));
        }
    }
    @Test void rechazaJsonAusenteIncompletoONumerosInvalidos() {
        for(String json:new String[]{"{}","{\"route\":null}","{\"route\":{\"distance\":-1,\"duration\":9}}","{\"route\":{\"duration\":9}}","json roto"}) {
            cuerpo.set(json);assertThrows(ResponseStatusException.class,()->cliente.consultar(0,0,0,0));
        }
    }
}
