package com.urbangardens.parcelservice.util;

import com.urbangardens.parcelservice.exception.InvalidParcelDataException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Utilidades de sanitización aplicadas <b>antes</b> de persistir datos de texto libre.
 *
 * Estrategia de defensa en profundidad:
 *   1. Bean Validation rechaza formatos incorrectos en el DTO.
 *   2. Esta clase limpia y verifica los valores que superaron la validación
 *      (segunda línea de defensa contra inyecciones XSS / SQL / NoSQL).
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SanitizationUtil {

    // Compiled once at class load — reusing Pattern instances is way faster than compiling per call
    private static final Pattern HTML_TAGS       = Pattern.compile("<[^>]+>");
    // (?i) = case-insensitive, \b = word boundary so "SELECTOR" doesn't match "SELECT"
    private static final Pattern SQL_KEYWORDS    = Pattern.compile(
        "(?i)(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE|EXEC|UNION|--|;)\\b)");
    // catches: javascript:, onclick=, onmouseover =, <script, < script (with space)
    private static final Pattern SCRIPT_EVENTS   = Pattern.compile(
        "(?i)(javascript:|on\\w+\\s*=|<\\s*script)");
    private static final Pattern NULL_BYTES       = Pattern.compile("\0");
    private static final Pattern CONSECUTIVE_SPACES = Pattern.compile("\\s{2,}");

    // ------------------------------------------------------------------ //
    //  API pública                                                        //
    // ------------------------------------------------------------------ //

    /**
     * Sanitiza un campo de texto libre (nombre, responsable, etc.).
     *
     * @param value  Valor original del campo.
     * @param field  Nombre del campo (para mensajes de error).
     * @return Valor limpio y normalizado.
     * @throws InvalidParcelDataException si se detecta contenido malicioso tras la limpieza.
     */
    public static String sanitizeText(String value, String field) {
        if (value == null) {
            return null;
        }

        // 1. Eliminar bytes nulos y espacios en los extremos
        String sanitized = NULL_BYTES.matcher(value.trim()).replaceAll("");

        // 2. Comprimir espacios internos múltiples
        sanitized = CONSECUTIVE_SPACES.matcher(sanitized).replaceAll(" ");

        // 3. Eliminar etiquetas HTML
        sanitized = HTML_TAGS.matcher(sanitized).replaceAll("");

        // Steps 4-5: we throw instead of replacing — silently stripping "DROP TABLE"
        // could produce a valid-looking but corrupted value, better to just reject it
        if (SCRIPT_EVENTS.matcher(sanitized).find()) {
            log.warn("[SANITIZACIÓN] Intento de inyección de script detectado en campo '{}'", field);
            throw new InvalidParcelDataException(field,
                "el valor contiene código de script no permitido. " +
                "No se aceptan eventos JavaScript ni etiquetas <script>.");
        }

        if (SQL_KEYWORDS.matcher(sanitized).find()) {
            log.warn("[SANITIZACIÓN] Posible inyección SQL detectada en campo '{}'", field);
            throw new InvalidParcelDataException(field,
                "el valor contiene palabras reservadas de SQL que no están permitidas " +
                "en este campo (SELECT, DROP, INSERT, etc.).");
        }

        return sanitized;
    }

    /**
     * Valida que las coordenadas GPS correspondan a una ubicación en tierra firme
     * en Latinoamérica/España (rango aproximado). Esta es una validación de negocio
     * adicional a la validación de rango genérico del DTO.
     *
     * Rango permisivo: lat [-60, 40], lon [-120, 10]
     *
     * @throws InvalidParcelDataException si las coordenadas están fuera del rango de operación.
     */
    public static void validateOperationalCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return; // Bean Validation ya lo captura con @NotNull
        }

        // rough bounding box covering Latin America + Spain — not a precise border check,
        // just a sanity guard against someone sending coordinates in the middle of the Pacific
        boolean latOutOfRange = latitude < -60.0 || latitude > 40.0;
        boolean lonOutOfRange = longitude < -120.0 || longitude > 10.0;

        if (latOutOfRange || lonOutOfRange) {
            log.warn("[COORDENADAS] Ubicación fuera del área de operación: lat={}, lon={}",
                    latitude, longitude);
            throw new InvalidParcelDataException("location",
                String.format(
                    "las coordenadas GPS (lat=%.6f, lon=%.6f) están fuera del área de operación " +
                    "reconocida por la red de huertos (Latinoamérica y España). " +
                    "Verifique que los valores sean correctos.", latitude, longitude));
        }
    }
}
