package cl.sanaesencia.usuarios.dto;

import cl.sanaesencia.usuarios.model.Rol;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CambiarRolRequest(

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        @Positive(message = "El ID del especialista debe ser mayor a cero")
        Long especialistaId

) {
}
