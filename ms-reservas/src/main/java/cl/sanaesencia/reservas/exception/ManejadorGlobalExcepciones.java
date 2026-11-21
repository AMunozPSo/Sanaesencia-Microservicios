package cl.sanaesencia.reservas.exception;

import cl.sanaesencia.reservas.dto.ErrorResponse;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarParametroIncorrecto(MethodArgumentTypeMismatchException error) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400,
                "El parámetro no tiene un formato válido", Map.of(error.getName(), "Valor incorrecto")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarConflictoDeDatos(DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(409,
                "La operación entra en conflicto con un registro existente", Map.of()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> manejarErrorDelServicio(
            ResponseStatusException excepcion
    ) {
        String mensaje = excepcion.getReason() != null
                ? excepcion.getReason()
                : "No se pudo completar la solicitud";

        ErrorResponse respuesta = new ErrorResponse(
                excepcion.getStatusCode().value(),
                mensaje,
                Map.of()
        );

        return ResponseEntity
                .status(excepcion.getStatusCode())
                .body(respuesta);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionDelDto(
            MethodArgumentNotValidException excepcion
    ) {
        Map<String, String> errores = new LinkedHashMap<>();

        excepcion.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errores.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Revisa los datos enviados",
                        errores
                )
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> manejarValidacionDelServicio(
            ConstraintViolationException excepcion
    ) {
        Map<String, String> errores = new LinkedHashMap<>();

        excepcion.getConstraintViolations().forEach(error -> {
            String ruta = error.getPropertyPath().toString();
            String campo = ruta.substring(ruta.lastIndexOf('.') + 1);

            errores.putIfAbsent(campo, error.getMessage());
        });

        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Revisa los datos enviados",
                        errores
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIncorrecto(
            HttpMessageNotReadableException excepcion
    ) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "El cuerpo de la solicitud no tiene un formato válido",
                        Map.of()
                )
        );
    }
}
