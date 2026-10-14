package com.urbangardens.parcelservice.exception;

import java.util.UUID;

/**
 * Lanzada cuando se solicita una parcela que no existe en la base de datos.
 */
public class ParcelNotFoundException extends RuntimeException {

    private final UUID parcelId;

    public ParcelNotFoundException(UUID id) {
        super(String.format(
            "No se encontró ninguna parcela con el identificador '%s'. " +
            "Verifique que el ID sea correcto o consulte el listado general de parcelas.",
            id));
        this.parcelId = id;
    }

    public UUID getParcelId() {
        return parcelId;
    }
}
