package cl.sanaesencia.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroUsuarioRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido admite hasta 100 caracteres")
        String apellido,

        @NotBlank(message = "El RUN es obligatorio")
        @Pattern(
                regexp = "^[0-9]{7,8}-[0-9kK]$",
                message = "El RUN debe ir sin puntos y con guion, por ejemplo 12345678-5"
        )
        String run,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo admite hasta 254 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(
                min = 8,
                max = 64,
                message = "La contraseña debe tener entre 8 y 64 caracteres"
        )
        String password

) {
}
