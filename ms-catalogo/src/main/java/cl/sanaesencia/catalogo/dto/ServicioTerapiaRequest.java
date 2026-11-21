package cl.sanaesencia.catalogo.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ServicioTerapiaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(
                max = 150,
                message = "El nombre admite hasta 150 caracteres"
        )
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(
                max = 2000,
                message = "La descripción admite hasta 2000 caracteres"
        )
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(
                value = "0.01",
                message = "El precio debe ser mayor a cero"
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "El precio admite hasta 8 enteros y 2 decimales"
        )
        BigDecimal precio,

        @NotNull(message = "La duración es obligatoria")
        @Positive(message = "La duración debe ser mayor a cero")
        Integer duracionMinutos,

        @NotNull(message = "Debes indicar si permite atención online")
        Boolean permiteOnline

) {
}
