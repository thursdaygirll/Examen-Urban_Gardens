package com.urbangardens.parcelservice.entity;

/**
 * Tipos de suelo reconocidos por el sistema.
 * Cada valor corresponde a una clasificación estándar agrícola.
 */
public enum SoilType {
    ARCILLOSO("Arcilloso"),
    LIMOSO("Limoso"),
    ARENOSO("Arenoso"),
    FRANCO("Franco"),
    FRANCO_ARCILLOSO("Franco-Arcilloso"),
    FRANCO_ARENOSO("Franco-Arenoso");

    private final String displayName;

    SoilType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
