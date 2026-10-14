package com.urbangardens.parcelservice.service;

import com.urbangardens.parcelservice.dto.ParcelRequestDTO;
import com.urbangardens.parcelservice.dto.ParcelResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Contrato del servicio de gestión de parcelas.
 */
public interface ParcelService {

    /**
     * Registra una nueva parcela en el sistema.
     *
     * @param request DTO validado y sanitizado con los datos de la parcela.
     * @return Representación de la parcela creada.
     * @throws com.urbangardens.parcelservice.exception.DuplicateParcelNameException
     *         si ya existe una parcela con el mismo nombre.
     * @throws com.urbangardens.parcelservice.exception.InvalidParcelDataException
     *         si los datos contienen valores lógicamente incoherentes.
     */
    ParcelResponseDTO createParcel(ParcelRequestDTO request);

    /**
     * Retorna el listado completo de parcelas registradas.
     *
     * @return Lista (puede estar vacía) de parcelas.
     */
    List<ParcelResponseDTO> getAllParcels();

    /**
     * Retorna el detalle de una parcela específica.
     *
     * @param id Identificador UUID de la parcela.
     * @return Datos de la parcela.
     * @throws com.urbangardens.parcelservice.exception.ParcelNotFoundException
     *         si no existe ninguna parcela con ese ID.
     */
    ParcelResponseDTO getParcelById(UUID id);
}
