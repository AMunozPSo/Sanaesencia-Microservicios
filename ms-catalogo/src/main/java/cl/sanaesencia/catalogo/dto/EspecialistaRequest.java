package cl.sanaesencia.catalogo.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

public record EspecialistaRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
        @NotBlank(message = "La especialidad es obligatoria") @Size(max = 200) String especialidad,
        @NotNull(message = "La descripción es obligatoria") @Size(max = 2000) String descripcion,
        @NotNull(message = "Indica si permite atención presencial") Boolean permitePresencial,
        @NotNull(message = "Indica si permite atención online") Boolean permiteOnline,
        @NotEmpty(message = "Asigna al menos un servicio")
        Set<@NotNull @Positive Long> servicioIds
) {}
