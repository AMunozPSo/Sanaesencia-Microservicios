package cl.sanaesencia.catalogo.service;

import cl.sanaesencia.catalogo.model.*;
import cl.sanaesencia.catalogo.repository.*;
import cl.sanaesencia.catalogo.seed.CatalogoCentroData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
public class ImportacionCatalogoService {
    private final ServicioTerapiaRepository servicios;
    private final EspecialistaRepository especialistas;
    public ImportacionCatalogoService(ServicioTerapiaRepository servicios, EspecialistaRepository especialistas) {
        this.servicios = servicios;
        this.especialistas = especialistas;
    }

    public record Resultado(int serviciosCreados, int especialistasCreados,
                            int serviciosExistentes, int especialistasExistentes) {}

    @Transactional
    public Resultado importarCentro() {
        int serviciosCreados = 0;
        int especialistasCreados = 0;
        Map<String, ServicioTerapia> porClave = new LinkedHashMap<>();
        for (var area : CatalogoCentroData.AREAS) {
            ServicioTerapia servicio = servicios.findByNombreIgnoreCase(area.nombre()).orElse(null);
            if (servicio == null) {
                servicio = new ServicioTerapia();
                servicio.setNombre(area.nombre());
                servicio.setDescripcion("Atención de " + area.nombre() + ".");
                servicio.setPrecio(new BigDecimal("30000.00"));
                servicio.setDuracionMinutos(60);
                servicio.setPermiteOnline(area.online());
                servicio = servicios.saveAndFlush(servicio);
                serviciosCreados++;
            }
            porClave.put(area.clave(), servicio);
        }
        for (var perfil : CatalogoCentroData.PROFESIONALES) {
            if (especialistas.findByNombreIgnoreCase(perfil.nombre()).isPresent()) continue;
            Especialista especialista = new Especialista();
            especialista.setNombre(perfil.nombre());
            especialista.setEspecialidad(perfil.especialidad());
            especialista.setDescripcion(perfil.pausado() ? "Agenda pausada · próximo retorno" : "");
            especialista.setPermitePresencial(!perfil.soloOnline());
            Set<ServicioTerapia> asignados = new LinkedHashSet<>();
            perfil.areas().forEach(clave -> asignados.add(porClave.get(clave)));
            especialista.setServicios(asignados);
            especialista.setPermiteOnline(asignados.stream().anyMatch(ServicioTerapia::isPermiteOnline));
            especialista.setActivo(!perfil.pausado() && asignados.stream().anyMatch(s -> s.isActivo()
                    && (especialista.isPermitePresencial()
                    || (especialista.isPermiteOnline() && s.isPermiteOnline()))));
            especialistas.saveAndFlush(especialista);
            especialistasCreados++;
        }
        return new Resultado(serviciosCreados, especialistasCreados,
                CatalogoCentroData.AREAS.size() - serviciosCreados,
                CatalogoCentroData.PROFESIONALES.size() - especialistasCreados);
    }
}
