package cl.sanaesencia.reservas.dto;
import cl.sanaesencia.reservas.model.Modalidad;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
public record ReservaRequest(
    @NotNull @Positive Long servicioId,
    @NotNull @Positive Long especialistaId,
    @NotNull LocalDateTime inicio,
    @NotNull Modalidad modalidad,
    @DecimalMin("-90") @DecimalMax("90") Double latitud,
    @DecimalMin("-180") @DecimalMax("180") Double longitud
) {
    @AssertTrue(message="Envía latitud y longitud juntas")
    public boolean isCoordenadasCompletas() { return (latitud==null)==(longitud==null); }
}
