package com.urbangardens.parcelservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad principal que representa una parcela / zona de cultivo física
 * monitorizada dentro de la red de huertos urbanos.
 */
@Entity
@Table(
    name = "parcels",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_parcel_name",
        columnNames = "name"
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Nombre descriptivo único de la parcela (p.ej. "Huerto Norte - Bloque A").
     */
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /**
     * Coordenadas GPS de la parcela, almacenadas como columnas embebidas.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude",  column = @Column(name = "gps_latitude",  nullable = false)),
        @AttributeOverride(name = "longitude", column = @Column(name = "gps_longitude", nullable = false))
    })
    private GpsLocation location;

    /**
     * Clasificación pedológica del suelo de la parcela.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "soil_type", nullable = false, length = 30)
    private SoilType soilType;

    /**
     * Nombre del administrador / líder responsable de la parcela.
     */
    @Column(name = "owner_or_lead", nullable = false, length = 150)
    private String ownerOrLead;

    /**
     * Marca temporal de registro; la genera el motor de base de datos.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
