package com.urbangardens.parcelservice.entity;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Coordenadas GPS embebidas en la entidad Parcel.
 * Latitud válida: -90 a +90. Longitud válida: -180 a +180.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GpsLocation {

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud mínima permitida es -90.0")
    @DecimalMax(value = "90.0",  message = "La latitud máxima permitida es 90.0")
    private Double latitude;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud mínima permitida es -180.0")
    @DecimalMax(value = "180.0",  message = "La longitud máxima permitida es 180.0")
    private Double longitude;
}
