package com.urbangardens.parcelservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estructura uniforme de respuesta de error devuelta al cliente.
 *
 * Campos opcionales (sólo presentes si aplican):
 *   - errors: lista de mensajes de validación por campo.
 *   - details: información adicional del recurso afectado.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** Código HTTP (p.ej. 400, 404, 409, 500). */
    private int status;

    /** Nombre corto del tipo de error (p.ej. "NOT_FOUND"). */
    private String error;

    /** Mensaje principal legible por el usuario. */
    private String message;

    /** Ruta de la solicitud que originó el error. */
    private String path;

    /** Instante en que ocurrió el error (UTC). */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Lista de errores de validación por campo (sólo en 400). */
    private List<FieldError> errors;

    // ------------------------------------------------------------------ //

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {

        /** Nombre del campo con el error. */
        private String field;

        /** Valor rechazado (puede ser null). */
        private Object rejectedValue;

        /** Descripción del motivo del rechazo. */
        private String message;
    }
}
