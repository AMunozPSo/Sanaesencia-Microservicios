package cl.sanaesencia.reservas.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;
public record AgendaRequest(@NotEmpty @Size(max=28) List<@Valid Franja> horarios) {
    public record Franja(@NotNull DayOfWeek dia, @NotNull LocalTime desde, @NotNull LocalTime hasta) {}
}
