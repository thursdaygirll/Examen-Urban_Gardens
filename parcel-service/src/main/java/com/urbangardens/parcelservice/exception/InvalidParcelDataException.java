package com.urbangardens.parcelservice.exception;

/**
 * Lanzada cuando los datos de una parcela superan la validación de formato
 * pero contienen valores lógicamente incoherentes (p.ej. coordenadas en el
 * océano para un huerto urbano que debe estar en tierra firme).
 *
 * También se usa como excepción de sanitización cuando se detecta contenido
 * potencialmente malicioso tras la limpieza manual.
 */
public class InvalidParcelDataException extends RuntimeException {

    private final String field;

    public InvalidParcelDataException(String field, String reason) {
        super(String.format(
            "El campo '%s' contiene un valor inválido: %s", field, reason));
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
