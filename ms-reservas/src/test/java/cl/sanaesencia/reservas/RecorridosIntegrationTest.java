package cl.sanaesencia.reservas;
import cl.sanaesencia.reservas.client.*;
import cl.sanaesencia.reservas.dto.Cuenta;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.concurrent.atomic.AtomicLong;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class RecorridosIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean UsuariosClient usuarios;
    @MockitoBean TrueWayClient proveedor;
    static final AtomicLong ids=new AtomicLong(1000);
    @BeforeEach void preparar(){
        long id=ids.incrementAndGet();
        when(usuarios.validarCredenciales(anyString(),anyString())).thenReturn(new Cuenta(id,"fixture@example.com","Paciente","Prueba","PACIENTE",null,true));
        when(proveedor.consultar(anyDouble(),anyDouble(),anyDouble(),anyDouble())).thenReturn(new TrueWayClient.Respuesta(new TrueWayClient.Ruta(1825.0,369.0)));
    }
    @Test void exigeSesionAntesDeConsultarUnProveedorDePago() throws Exception {
        mvc.perform(post("/api/rutas/sede").contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-31.9,\"longitud\":-71.5}"))
            .andExpect(status().isUnauthorized());verifyNoInteractions(proveedor);
    }
    @Test void devuelveSoloDistanciaYDuracionSinAlmacenarEnCache() throws Exception {
        mvc.perform(post("/api/rutas/sede").with(httpBasic("fixture@example.com","clave-fixture"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-31.9,\"longitud\":-71.5}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.route.distance").value(1825))
            .andExpect(jsonPath("$.route.duration").value(369)).andExpect(jsonPath("$.clave").doesNotExist())
            .andExpect(header().string("Cache-Control","no-store"));
        verify(proveedor).consultar(-31.9,-71.5,-31.909825,-71.512501);
    }
    @Test void rechazaCoordenadasInvalidasSinConsumirProveedor() throws Exception {
        for(String cuerpo:new String[]{"{}","{\"latitud\":91,\"longitud\":0}","{\"latitud\":0,\"longitud\":181}"})
            mvc.perform(post("/api/rutas/sede").with(httpBasic("fixture@example.com","clave-fixture"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isBadRequest());
        verifyNoInteractions(proveedor);
    }
    @Test void sextaConsultaDeLaCuentaSeRechazaAntesDeIrAlProveedor() throws Exception {
        for(int i=0;i<6;i++)
            mvc.perform(post("/api/rutas/sede").with(httpBasic("fixture@example.com","clave-fixture"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitud\":-31.9,\"longitud\":-71.5}"))
                .andExpect(status().is(i<5?200:429));
        verify(proveedor,times(5)).consultar(anyDouble(),anyDouble(),anyDouble(),anyDouble());
    }
}
