package com.urbangardens.parcelservice.exception;

/**
 * Lanzada cuando se intenta registrar una parcela con un nombre
 * que ya está en uso en el sistema.
 */
public class DuplicateParcelNameException extends RuntimeException {

    private final String duplicateName;

    public DuplicateParcelNameException(String name) {
        super(String.format(
            "Ya existe una parcela registrada con el nombre '%s'. " +
            "Por favor, elija un nombre distinto para identificar de forma única esta zona de cultivo.",
            name));
        this.duplicateName = name;
    }

    public String getDuplicateName() {
        return duplicateName;
    }
}
