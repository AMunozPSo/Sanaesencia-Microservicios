package cl.sanaesencia.reservas.client;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
@Component
public class CatalogoClient {
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Servicio(Long id,String nombre,BigDecimal precio,Integer duracionMinutos,
                           boolean permiteOnline,boolean activo) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Perfil(Long id,String nombre,boolean activo,boolean permitePresencial,
                         boolean permiteOnline,List<Servicio> servicios) {}
    public record Atencion(Perfil especialista, Servicio servicio) {}
    private final RestClient cliente;
    public CatalogoClient(@Value("${catalogo.api.url}") String url) {
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); factory.setReadTimeout(5000);
        cliente=RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
    public Perfil perfil(Long id) {
        Perfil perfil;
        try {
            perfil=cliente.get().uri("/api/especialistas/{id}",id).retrieve().body(Perfil.class);
        } catch(RestClientResponseException e) {
            if(e.getStatusCode().value()==404) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Especialista no encontrado");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"El catálogo no está disponible");
        } catch(RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"El catálogo no está disponible");
        }
        if(perfil==null||!id.equals(perfil.id())||perfil.nombre()==null||perfil.servicios()==null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Respuesta inválida del catálogo");
        if(!perfil.activo()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El especialista tiene su agenda pausada");
        return perfil;
    }
    public Atencion atencion(Long especialistaId,Long servicioId) {
        Perfil perfil=perfil(especialistaId);
        Servicio servicio=perfil.servicios().stream().filter(s->servicioId.equals(s.id())).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"El especialista no ofrece ese servicio"));
        if(!servicio.activo()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El servicio está desactivado");
        if(servicio.duracionMinutos()==null||servicio.duracionMinutos()<=0||servicio.precio()==null
                ||servicio.precio().signum()<=0)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"El catálogo tiene datos de atención inválidos");
        return new Atencion(perfil,servicio);
    }
}
