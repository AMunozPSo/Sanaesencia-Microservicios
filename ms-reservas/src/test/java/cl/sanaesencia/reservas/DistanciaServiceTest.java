package cl.sanaesencia.reservas;
import cl.sanaesencia.reservas.service.DistanciaService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
class DistanciaServiceTest {
    final DistanciaService distancia=new DistanciaService(-31.909825,-71.512501,20);
    @Test void mismaSedeEsCero() { assertEquals(0,distancia.calcular(-31.909825,-71.512501),0.00001); }
    @Test void distanciaLejanaEsMayorAlUmbral() {
        double km=distancia.calcular(-33.45,-70.66);
        assertTrue(km>150&&km<250);
        assertTrue(km>distancia.umbral());
    }
    @Test void ubicacionAusenteNoSeInventa() { assertNull(distancia.calcular(null,null)); }
    @Test void coordenadasParcialesEInvalidasSonRechazadas() {
        assertThrows(ResponseStatusException.class,()->distancia.calcular(0.0,null));
        assertThrows(ResponseStatusException.class,()->distancia.calcular(91.0,0.0));
        assertThrows(ResponseStatusException.class,()->distancia.calcular(0.0,181.0));
        assertThrows(ResponseStatusException.class,()->distancia.calcular(Double.NaN,0.0));
    }
}
