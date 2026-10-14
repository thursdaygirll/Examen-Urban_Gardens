package com.urbangardens.parcelservice.dto;

import com.urbangardens.parcelservice.entity.SoilType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para crear una nueva parcela.
 * Aplica validación declarativa (Bean Validation) y anotaciones
 * de sanitización antes de que los datos lleguen a la capa de servicio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParcelRequestDTO {

    /**
     * Nombre único de la parcela.
     * - No puede estar vacío ni contener sólo espacios.
     * - Longitud entre 3 y 120 caracteres.
     * - Sólo letras, números, guiones, puntos y espacios (se rechaza HTML/SQL injection).
     */
    @NotBlank(message = "El nombre de la parcela es obligatorio y no puede estar vacío")
    @Size(min = 3, max = 120,
          message = "El nombre debe tener entre 3 y 120 caracteres")
    // \p{L} = any Unicode letter, \p{N} = any Unicode digit — handles accented chars across locales
    // explicit accented chars at the end are a fallback for environments where \p{L} isn't enough
    @Pattern(
        regexp = "^[\\p{L}\\p{N}\\s\\-\\.,'#áéíóúÁÉÍÓÚñÑüÜ]+$",
        message = "El nombre sólo puede contener letras, números, espacios y los caracteres: - . , ' #"
    )
    private String name;

    /**
     * Coordenadas GPS de la parcela.
     * El objeto anidado tiene sus propias restricciones (@Valid las activa).
     */
    @NotNull(message = "Las coordenadas GPS de la parcela son obligatorias")
    @Valid
    private GpsLocationDTO location;

    /**
     * Tipo de suelo (valor del enum SoilType).
     */
    @NotNull(message = "El tipo de suelo es obligatorio. Valores válidos: ARCILLOSO, LIMOSO, ARENOSO, FRANCO, FRANCO_ARCILLOSO, FRANCO_ARENOSO")
    private SoilType soilType;

    /**
     * Nombre completo del administrador o líder responsable.
     * - Sólo letras, espacios y caracteres propios de nombres hispanos.
     */
    @NotBlank(message = "El nombre del responsable de la parcela es obligatorio")
    @Size(min = 3, max = 150,
          message = "El nombre del responsable debe tener entre 3 y 150 caracteres")
    @Pattern(
        regexp = "^[\\p{L}\\s\\-\\.áéíóúÁÉÍÓÚñÑüÜ]+$",
        message = "El nombre del responsable sólo puede contener letras, espacios, guiones y puntos"
    )
    private String ownerOrLead;

    // ------------------------------------------------------------------ //
    //  DTO anidado para la ubicación GPS                                  //
    // ------------------------------------------------------------------ //

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GpsLocationDTO {

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", inclusive = true,
                    message = "La latitud no puede ser menor a -90.0")
        @DecimalMax(value = "90.0",  inclusive = true,
                    message = "La latitud no puede ser mayor a 90.0")
        private Double latitude;

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", inclusive = true,
                    message = "La longitud no puede ser menor a -180.0")
        @DecimalMax(value = "180.0",  inclusive = true,
                    message = "La longitud no puede ser mayor a 180.0")
        private Double longitude;
    }
}
