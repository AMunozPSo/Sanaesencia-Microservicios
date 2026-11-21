package cl.sanaesencia.reservas.dto;
import cl.sanaesencia.reservas.model.EstadoReserva;
import jakarta.validation.constraints.NotNull;
public record EstadoReservaRequest(@NotNull EstadoReserva estado) {}
