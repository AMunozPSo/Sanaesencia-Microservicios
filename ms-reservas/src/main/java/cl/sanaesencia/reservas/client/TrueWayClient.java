package cl.sanaesencia.reservas.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

/** Cliente exclusivo del proveedor: nunca recibe la autorización del paciente. */
@Component
public class TrueWayClient {
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Respuesta(Ruta route) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Ruta(Double distance, Double duration) {}
    private final RestClient cliente;
    private final String clave;
    @org.springframework.beans.factory.annotation.Autowired
    public TrueWayClient(@Value("${trueway.api.key:}") String clave) {
        this(clave,"https://trueway-directions2.p.rapidapi.com");
    }
    TrueWayClient(String clave,String base) {
        this.clave=clave.strip();
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);factory.setReadTimeout(8000);
        cliente=RestClient.builder().baseUrl(base).requestFactory(factory).build();
    }
    public Respuesta consultar(double latitud,double longitud,double destinoLat,double destinoLon) {
        if(clave.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"El servicio de recorridos no está configurado");
        Respuesta respuesta;
        try {
            String puntos=latitud+","+longitud+";"+destinoLat+","+destinoLon;
            respuesta=cliente.get().uri(b->b.path("/FindDrivingRoute").queryParam("stops",puntos).build())
                .header("X-RapidAPI-Host","trueway-directions2.p.rapidapi.com")
                .header("X-RapidAPI-Key",clave).retrieve().body(Respuesta.class);
        } catch(RestClientResponseException error) {
            if(error.getStatusCode().value()==429)
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Se alcanzó el límite de consultas de recorridos");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"El proveedor de recorridos no está disponible");
        } catch(RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo consultar el recorrido");
        }
        if(respuesta==null||respuesta.route()==null||respuesta.route().distance()==null||respuesta.route().duration()==null
                ||!Double.isFinite(respuesta.route().distance())||!Double.isFinite(respuesta.route().duration())
                ||respuesta.route().distance()<0||respuesta.route().duration()<0)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"No se encontró un recorrido válido para esa ubicación");
        return respuesta;
    }
}
