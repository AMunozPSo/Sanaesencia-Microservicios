package cl.sanaesencia.reservas.dto;
import jakarta.validation.constraints.*;
public record BoxRequest(@NotBlank @Size(max=60) String nombre,
                         @NotNull @Size(max=300) String descripcion) {}
