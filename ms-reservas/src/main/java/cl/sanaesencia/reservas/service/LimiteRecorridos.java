package cl.sanaesencia.reservas.service;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Presupuesto de la instancia: cuenta los intentos al proveedor, sin guardar ubicaciones. */
@Service
public class LimiteRecorridos {
    private record Ventana(long minuto,int cantidad) {}
    private final Clock reloj;
    private final int porMinuto,porDia;
    private final Map<Long,Ventana> cuentas=new HashMap<>();
    private LocalDate dia;
    private int usados;
    public LimiteRecorridos(Clock reloj,@Value("${trueway.limite-minuto:5}") int porMinuto,
            @Value("${trueway.limite-dia:400}") int porDia) {
        this.reloj=reloj;this.porMinuto=porMinuto;this.porDia=porDia;
    }
    public synchronized void consumir(Long usuario) {
        var ahora=reloj.instant();var hoy=ahora.atZone(ZoneOffset.UTC).toLocalDate();
        if(!hoy.equals(dia)){dia=hoy;usados=0;cuentas.clear();}
        long minuto=ahora.getEpochSecond()/60;
        cuentas.entrySet().removeIf(e->e.getValue().minuto()!=minuto);
        var anterior=cuentas.get(usuario);
        int cantidad=anterior==null?0:anterior.cantidad();
        if(usados>=porDia||cantidad>=porMinuto)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Se alcanzó el límite de consultas de recorridos. Inténtalo más tarde");
        cuentas.put(usuario,new Ventana(minuto,cantidad+1));usados++;
    }
}
