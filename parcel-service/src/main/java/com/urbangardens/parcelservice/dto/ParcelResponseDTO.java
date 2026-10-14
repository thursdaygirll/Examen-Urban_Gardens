package com.urbangardens.parcelservice.dto;

import com.urbangardens.parcelservice.entity.SoilType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida que expone los datos de una parcela al cliente.
 * Nunca expone la entidad JPA directamente; se proyecta aquí.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelResponseDTO {

    private UUID id;
    private String name;
    private GpsLocationDTO location;
    private SoilType soilType;
    private String soilTypeDisplayName;
    private String ownerOrLead;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GpsLocationDTO {
        private Double latitude;
        private Double longitude;
    }
}
