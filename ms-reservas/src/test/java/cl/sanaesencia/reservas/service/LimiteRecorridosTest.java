package cl.sanaesencia.reservas.service;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class LimiteRecorridosTest {
    static class Reloj extends Clock {
        Instant ahora=Instant.parse("2026-11-22T12:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId z){return this;}
        public Instant instant(){return ahora;}
    }
    @Test void limitaPorCuentaYReiniciaElMinutoSinBorrarElPresupuestoDiario() {
        var reloj=new Reloj();var limite=new LimiteRecorridos(reloj,1,2);
        limite.consumir(1L);assertEquals(429,assertThrows(ResponseStatusException.class,()->limite.consumir(1L)).getStatusCode().value());
        reloj.ahora=reloj.ahora.plusSeconds(60);limite.consumir(1L);
        assertThrows(ResponseStatusException.class,()->limite.consumir(2L));
        reloj.ahora=reloj.ahora.plusSeconds(86400);assertDoesNotThrow(()->limite.consumir(2L));
    }
    @Test void elLimiteGlobalIncluyeTodasLasCuentasYEsAtomico() throws Exception {
        var limite=new LimiteRecorridos(new Reloj(),5,2);var aceptadas=new AtomicInteger();
        var pool=Executors.newFixedThreadPool(6);
        try {
            var futuros=new java.util.ArrayList<Future<?>>();
            for(long id=1;id<=12;id++){final long cuenta=id;futuros.add(pool.submit(()->{try{limite.consumir(cuenta);aceptadas.incrementAndGet();}catch(ResponseStatusException e){assertEquals(429,e.getStatusCode().value());}}));}
            for(var f:futuros)f.get(5,TimeUnit.SECONDS);
            assertEquals(2,aceptadas.get());
        }finally{pool.shutdownNow();}
    }
}
