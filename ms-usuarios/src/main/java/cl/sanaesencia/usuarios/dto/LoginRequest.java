package cl.sanaesencia.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo admite hasta 254 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 64, message = "La contraseña admite hasta 64 caracteres")
        String password

) {
}
