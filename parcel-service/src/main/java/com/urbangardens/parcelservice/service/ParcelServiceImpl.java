package com.urbangardens.parcelservice.service;

import com.urbangardens.parcelservice.dto.ParcelRequestDTO;
import com.urbangardens.parcelservice.dto.ParcelResponseDTO;
import com.urbangardens.parcelservice.entity.GpsLocation;
import com.urbangardens.parcelservice.entity.Parcel;
import com.urbangardens.parcelservice.exception.DuplicateParcelNameException;
import com.urbangardens.parcelservice.exception.ParcelNotFoundException;
import com.urbangardens.parcelservice.repository.ParcelRepository;
import com.urbangardens.parcelservice.util.SanitizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementación del servicio de parcelas.
 *
 * Flujo de cada operación de escritura:
 *   DTO validado → sanitización manual → verificación de unicidad → persistencia
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelServiceImpl implements ParcelService {

    private final ParcelRepository parcelRepository;

    // ------------------------------------------------------------------ //
    //  Crear parcela                                                      //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public ParcelResponseDTO createParcel(ParcelRequestDTO request) {
        log.info("[PARCEL] Iniciando registro de nueva parcela: '{}'", request.getName());

        // 1. Sanitización manual (segunda línea de defensa)
        String sanitizedName  = SanitizationUtil.sanitizeText(request.getName(),        "name");
        String sanitizedOwner = SanitizationUtil.sanitizeText(request.getOwnerOrLead(), "ownerOrLead");

        // 2. Validación de coordenadas operativas
        SanitizationUtil.validateOperationalCoordinates(
                request.getLocation().getLatitude(),
                request.getLocation().getLongitude());

        // 3. Check uniqueness before saving — cheaper than catching a DB constraint violation,
        //    and gives us a cleaner error message than a DataIntegrityViolationException
        if (parcelRepository.existsByNameIgnoreCase(sanitizedName)) {
            throw new DuplicateParcelNameException(sanitizedName);
        }

        // 4. Construir y persistir la entidad
        Parcel parcel = new Parcel();
        parcel.setName(sanitizedName);
        parcel.setOwnerOrLead(sanitizedOwner);
        parcel.setSoilType(request.getSoilType());
        parcel.setLocation(new GpsLocation(
                request.getLocation().getLatitude(),
                request.getLocation().getLongitude()));

        Parcel saved = parcelRepository.save(parcel);
        log.info("[PARCEL] Parcela creada exitosamente. id={} nombre='{}'", saved.getId(), saved.getName());

        return toResponseDTO(saved);
    }

    // ------------------------------------------------------------------ //
    //  Listar todas las parcelas                                          //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional(readOnly = true) // tells Hibernate to skip dirty-checking — small perf win on read-only ops
    public List<ParcelResponseDTO> getAllParcels() {
        List<Parcel> parcels = parcelRepository.findAll();
        log.debug("[PARCEL] Consultando listado general. Total: {}", parcels.size());
        return parcels.stream().map(this::toResponseDTO).toList();
    }

    // ------------------------------------------------------------------ //
    //  Consultar parcela por ID                                           //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional(readOnly = true)
    public ParcelResponseDTO getParcelById(UUID id) {
        log.debug("[PARCEL] Buscando parcela con id={}", id);
        // orElseThrow keeps it one line and avoids an explicit null check
        Parcel parcel = parcelRepository.findById(id)
                .orElseThrow(() -> new ParcelNotFoundException(id));
        return toResponseDTO(parcel);
    }

    // ------------------------------------------------------------------ //
    //  Mapeo entidad → DTO                                                //
    // ------------------------------------------------------------------ //

    private ParcelResponseDTO toResponseDTO(Parcel parcel) {
        ParcelResponseDTO.GpsLocationDTO locationDTO = ParcelResponseDTO.GpsLocationDTO.builder()
                .latitude(parcel.getLocation().getLatitude())
                .longitude(parcel.getLocation().getLongitude())
                .build();

        return ParcelResponseDTO.builder()
                .id(parcel.getId())
                .name(parcel.getName())
                .location(locationDTO)
                .soilType(parcel.getSoilType())
                .soilTypeDisplayName(parcel.getSoilType().getDisplayName())
                .ownerOrLead(parcel.getOwnerOrLead())
                .createdAt(parcel.getCreatedAt())
                .build();
    }
}
