package com.urbangardens.parcelservice.repository;

import com.urbangardens.parcelservice.entity.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio JPA para la entidad {@link Parcel}.
 * Spring Data genera la implementación en tiempo de ejecución.
 */
@Repository
public interface ParcelRepository extends JpaRepository<Parcel, UUID> {

    /**
     * Verifica si ya existe una parcela con el nombre dado
     * (la comparación es sensible a mayúsculas/minúsculas a nivel de BD).
     */
    boolean existsByNameIgnoreCase(String name);
}
