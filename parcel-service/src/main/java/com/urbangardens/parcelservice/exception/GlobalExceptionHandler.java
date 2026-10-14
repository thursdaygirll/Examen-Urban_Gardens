package com.urbangardens.parcelservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * Manejador global de excepciones.
 *
 * Convierte cualquier excepción lanzada en la capa de servicio o controlador
 * en una respuesta HTTP uniforme usando {@link ErrorResponse}.
 *
 * Orden de prioridad de manejo:
 *   1. Excepciones de negocio personalizadas  (4xx específicos)
 *   2. Excepciones de validación de Spring     (400)
 *   3. Excepciones de infraestructura / JSON  (400 / 404)
 *   4. Catch-all general                       (500)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ //
    //  1. Excepciones de negocio personalizadas                           //
    // ------------------------------------------------------------------ //

    /**
     * 404 — La parcela solicitada no existe.
     */
    @ExceptionHandler(ParcelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleParcelNotFound(
            ParcelNotFoundException ex, HttpServletRequest request) {

        log.warn("[PARCELA NO ENCONTRADA] id={} | path={}", ex.getParcelId(), request.getRequestURI());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("PARCELA_NO_ENCONTRADA")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * 409 — Nombre de parcela duplicado.
     */
    @ExceptionHandler(DuplicateParcelNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateName(
            DuplicateParcelNameException ex, HttpServletRequest request) {

        log.warn("[NOMBRE DUPLICADO] nombre='{}' | path={}", ex.getDuplicateName(), request.getRequestURI());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("NOMBRE_DUPLICADO")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * 422 — Datos de la parcela lógicamente inválidos o contenido malicioso detectado.
     */
    @ExceptionHandler(InvalidParcelDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidData(
            InvalidParcelDataException ex, HttpServletRequest request) {

        log.warn("[DATO INVÁLIDO] campo='{}' | path={}", ex.getField(), request.getRequestURI());

        ErrorResponse.FieldError fieldError = ErrorResponse.FieldError.builder()
                .field(ex.getField())
                .message(ex.getMessage())
                .build();

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("DATO_INVALIDO")
                .message("Uno o más campos contienen valores lógicamente incorrectos.")
                .path(request.getRequestURI())
                .errors(List.of(fieldError))
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // ------------------------------------------------------------------ //
    //  2. Errores de validación declarativa (Bean Validation / @Valid)    //
    // ------------------------------------------------------------------ //

    /**
     * 400 — Uno o más campos no superaron la validación de formato/restricciones.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // getBindingResult() holds every failed constraint — we map all of them so the
        // client can fix everything in one shot instead of one error at a time
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue())
                        .message(fe.getDefaultMessage()) // comes from the message= in each annotation
                        .build())
                .toList();

        log.warn("[VALIDACIÓN FALLIDA] {} error(es) | path={}", fieldErrors.size(), request.getRequestURI());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("SOLICITUD_INVALIDA")
                .message("La solicitud contiene " + fieldErrors.size() +
                         " error(es) de validación. Revise los campos indicados.")
                .path(request.getRequestURI())
                .errors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    // ------------------------------------------------------------------ //
    //  3. Errores de infraestructura / HTTP                               //
    // ------------------------------------------------------------------ //

    /**
     * 400 — El cuerpo JSON es ilegible o el valor de un enum es incorrecto.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("[JSON INVÁLIDO] path={} | causa={}", request.getRequestURI(), ex.getMessage());

        // Jackson throws this for malformed JSON *and* for unknown enum values,
        // so we peek at the message to give a more specific hint when it's the enum
        String mensaje = ex.getMessage() != null && ex.getMessage().contains("SoilType")
                ? "El valor proporcionado para 'soilType' no es válido. " +
                  "Valores aceptados: ARCILLOSO, LIMOSO, ARENOSO, FRANCO, FRANCO_ARCILLOSO, FRANCO_ARENOSO."
                : "El cuerpo de la solicitud no es un JSON válido o está incompleto. " +
                  "Verifique la estructura y el tipo de datos enviados.";

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("CUERPO_ILEGIBLE")
                .message(mensaje)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 400 — El tipo del parámetro de ruta o consulta no coincide (p.ej. UUID malformado).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String mensaje = String.format(
            "El parámetro '%s' tiene un formato incorrecto. " +
            "Se esperaba un valor de tipo '%s', pero se recibió: '%s'.",
            ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido",
            ex.getValue());

        log.warn("[TIPO INCORRECTO] param='{}' | path={}", ex.getName(), request.getRequestURI());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("PARAMETRO_INVALIDO")
                .message(mensaje)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 404 — Ruta no encontrada (endpoint inexistente).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(
            NoHandlerFoundException ex, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("RUTA_NO_ENCONTRADA")
                .message(String.format(
                    "El recurso '%s %s' no existe en esta API. " +
                    "Consulte la documentación para conocer los endpoints disponibles.",
                    ex.getHttpMethod(), ex.getRequestURL()))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ------------------------------------------------------------------ //
    //  4. Catch-all: error interno no anticipado                          //
    // ------------------------------------------------------------------ //

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {

        log.error("[ERROR INTERNO] path={} | {}: {}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("ERROR_INTERNO")
                .message("Se produjo un error interno en el servidor. " +
                         "El equipo técnico ha sido notificado. Intente de nuevo más tarde.")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.internalServerError().body(body);
    }
}
