package cl.sanaesencia.reservas.dto;
import jakarta.validation.constraints.NotNull;
public record EstadoRequest(@NotNull Boolean activo) {}
