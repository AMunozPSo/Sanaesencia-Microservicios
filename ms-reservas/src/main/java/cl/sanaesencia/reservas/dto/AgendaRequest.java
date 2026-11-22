package cl.sanaesencia.reservas.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;
public record AgendaRequest(@NotEmpty @Size(max=140) List<@Valid Franja> horarios) {
    public record Franja(@NotNull DayOfWeek dia, @NotNull LocalTime desde, @NotNull LocalTime hasta, LocalDate fecha,Boolean cupoPublicado) {
        public Franja {cupoPublicado=Boolean.TRUE.equals(cupoPublicado);}
        public Franja(DayOfWeek dia,LocalTime desde,LocalTime hasta) {this(dia,desde,hasta,null,false);}
        public Franja(DayOfWeek dia,LocalTime desde,LocalTime hasta,LocalDate fecha) {this(dia,desde,hasta,fecha,false);}
    }
}
