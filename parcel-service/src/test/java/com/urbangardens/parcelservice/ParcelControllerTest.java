package com.urbangardens.parcelservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbangardens.parcelservice.dto.ParcelRequestDTO;
import com.urbangardens.parcelservice.entity.SoilType;
import com.urbangardens.parcelservice.exception.DuplicateParcelNameException;
import com.urbangardens.parcelservice.exception.ParcelNotFoundException;
import com.urbangardens.parcelservice.service.ParcelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@DisplayName("ParcelController — Validación, sanitización y manejo de errores")
class ParcelControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    ParcelService parcelService;

    // ------------------------------------------------------------------ //
    //  POST — errores de validación                                       //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("POST /parcels → 400 cuando el nombre está vacío")
    void createParcel_emptyName_returns400() throws Exception {
        String body = """
                {
                  "name": "",
                  "location": { "latitude": 19.4, "longitude": -99.1 },
                  "soilType": "ARCILLOSO",
                  "ownerOrLead": "María López"
                }
                """;
        mvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    @DisplayName("POST /parcels → 400 cuando la latitud supera el rango")
    void createParcel_latitudeOutOfRange_returns400() throws Exception {
        String body = """
                {
                  "name": "Huerto Norte",
                  "location": { "latitude": 999.0, "longitude": -99.1 },
                  "soilType": "ARCILLOSO",
                  "ownerOrLead": "María López"
                }
                """;
        mvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("location.latitude"));
    }

    @Test
    @DisplayName("POST /parcels → 400 cuando el tipo de suelo es inválido")
    void createParcel_invalidSoilType_returns400() throws Exception {
        String body = """
                {
                  "name": "Huerto Norte",
                  "location": { "latitude": 19.4, "longitude": -99.1 },
                  "soilType": "INVALIDO",
                  "ownerOrLead": "María López"
                }
                """;
        mvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CUERPO_ILEGIBLE"));
    }

    @Test
    @DisplayName("POST /parcels → 409 cuando el nombre ya existe")
    void createParcel_duplicateName_returns409() throws Exception {
        when(parcelService.createParcel(any()))
                .thenThrow(new DuplicateParcelNameException("Huerto Norte"));

        ParcelRequestDTO req = new ParcelRequestDTO(
                "Huerto Norte",
                new ParcelRequestDTO.GpsLocationDTO(19.4, -99.1),
                SoilType.ARCILLOSO,
                "María López");

        mvc.perform(post("/api/v1/parcels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NOMBRE_DUPLICADO"));
    }

    // ------------------------------------------------------------------ //
    //  GET — errores de consulta                                          //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("GET /parcels/{id} → 404 cuando la parcela no existe")
    void getParcelById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(parcelService.getParcelById(id))
                .thenThrow(new ParcelNotFoundException(id));

        mvc.perform(get("/api/v1/parcels/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PARCELA_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("GET /parcels/{id} → 400 cuando el ID no es un UUID válido")
    void getParcelById_malformedUuid_returns400() throws Exception {
        mvc.perform(get("/api/v1/parcels/no-es-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
    }
}
