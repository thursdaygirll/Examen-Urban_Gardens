package com.urbangardens.parcelservice.controller;

import com.urbangardens.parcelservice.dto.ParcelRequestDTO;
import com.urbangardens.parcelservice.dto.ParcelResponseDTO;
import com.urbangardens.parcelservice.service.ParcelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de parcelas.
 *
 * Base URL: /api/v1/parcels
 *
 * Endpoints:
 *   POST   /api/v1/parcels        → Registrar nueva parcela
 *   GET    /api/v1/parcels        → Listar todas las parcelas
 *   GET    /api/v1/parcels/{id}   → Consultar detalle de una parcela
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
public class ParcelController {

    private final ParcelService parcelService;

    // ------------------------------------------------------------------ //
    //  POST /api/v1/parcels                                               //
    // ------------------------------------------------------------------ //

    /**
     * Registra una nueva parcela en la red de huertos urbanos.
     *
     * <p>La anotación {@code @Valid} activa Bean Validation sobre el DTO;
     * cualquier violación lanza {@link org.springframework.web.bind.MethodArgumentNotValidException}
     * que el manejador global captura y convierte en un 400 estructurado.</p>
     *
     * @param request Datos de la parcela a crear.
     * @return 201 Created con el recurso generado.
     */
    @PostMapping
    public ResponseEntity<ParcelResponseDTO> createParcel(
            @Valid @RequestBody ParcelRequestDTO request) {

        log.info("[HTTP] POST /api/v1/parcels — nombre='{}'", request.getName()); // @Valid runs before this method body executes
        ParcelResponseDTO created = parcelService.createParcel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/parcels                                                //
    // ------------------------------------------------------------------ //

    /**
     * Retorna todas las parcelas registradas.
     *
     * @return 200 OK con la lista (vacía si no hay ninguna registrada).
     */
    @GetMapping
    public ResponseEntity<List<ParcelResponseDTO>> getAllParcels() {
        log.info("[HTTP] GET /api/v1/parcels");
        return ResponseEntity.ok(parcelService.getAllParcels());
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/parcels/{id}                                           //
    // ------------------------------------------------------------------ //

    /**
     * Retorna el detalle de una parcela específica.
     *
     * <p>Si el {@code id} no tiene formato UUID válido, Spring lanza
     * {@link org.springframework.web.method.annotation.MethodArgumentTypeMismatchException}
     * que el manejador global convierte en un 400 descriptivo.</p>
     *
     * @param id UUID de la parcela.
     * @return 200 OK con los datos de la parcela, o 404 si no existe.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ParcelResponseDTO> getParcelById(@PathVariable UUID id) {
        log.info("[HTTP] GET /api/v1/parcels/{}", id);
        return ResponseEntity.ok(parcelService.getParcelById(id));
    }
}
