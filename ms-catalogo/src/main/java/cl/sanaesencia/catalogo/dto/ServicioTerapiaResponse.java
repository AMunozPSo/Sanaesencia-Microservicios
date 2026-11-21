package cl.sanaesencia.catalogo.dto;

import cl.sanaesencia.catalogo.model.ServicioTerapia;

import java.math.BigDecimal;

public record ServicioTerapiaResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer duracionMinutos,
        boolean permiteOnline,
        boolean activo
) {

    public static ServicioTerapiaResponse desde(
            ServicioTerapia servicio
    ) {
        return new ServicioTerapiaResponse(
                servicio.getId(),
                servicio.getNombre(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getDuracionMinutos(),
                servicio.isPermiteOnline(),
                servicio.isActivo()
        );
    }
}
