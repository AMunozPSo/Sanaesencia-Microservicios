package cl.sanaesencia.catalogo.dto;

import cl.sanaesencia.catalogo.model.Especialista;
import java.util.Comparator;
import java.util.List;

public record EspecialistaResponse(
        Long id, String nombre, String especialidad, String descripcion,
        boolean permitePresencial, boolean permiteOnline, boolean activo,
        List<ServicioTerapiaResponse> servicios
) {
    public static EspecialistaResponse desde(Especialista especialista) {
        return new EspecialistaResponse(especialista.getId(), especialista.getNombre(),
                especialista.getEspecialidad(), especialista.getDescripcion(),
                especialista.isPermitePresencial(), especialista.isPermiteOnline(),
                especialista.isActivo(), especialista.getServicios().stream()
                .map(ServicioTerapiaResponse::desde)
                .sorted(Comparator.comparing(ServicioTerapiaResponse::id)).toList());
    }
}
