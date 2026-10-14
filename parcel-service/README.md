# Parcel Service — Red de Huertos Urbanos Comunitarios

Microservicio REST desarrollado con **Java 17 + Spring Boot 3** para la gestión de parcelas/zonas de cultivo físicas monitorizadas dentro de la red de huertos urbanos. Forma parte de una plataforma IoT más amplia que procesa datos de sensores de suelo y aire en tiempo real.

---

## Tabla de contenidos

1. [Contexto de negocio](#1-contexto-de-negocio)
2. [Stack tecnológico](#2-stack-tecnológico)
3. [Estructura del proyecto](#3-estructura-del-proyecto)
4. [Capas de la arquitectura](#4-capas-de-la-arquitectura)
   - [Entidades JPA](#41-entidades-jpa)
   - [DTOs](#42-dtos)
   - [Repositorio](#43-repositorio)
   - [Servicio](#44-servicio)
   - [Controlador REST](#45-controlador-rest)
5. [Validación y sanitización](#5-validación-y-sanitización)
6. [Manejo de errores y excepciones personalizadas](#6-manejo-de-errores-y-excepciones-personalizadas)
7. [API Reference](#7-api-reference)
8. [Ejecutar el proyecto](#8-ejecutar-el-proyecto)
9. [Tests](#9-tests)

---

## 1. Contexto de negocio

Una red de huertos urbanos recopila datos ambientales a través de sensores IoT (humedad del suelo, temperatura, pH, N/P/K, calidad del aire). Este microservicio gestiona las **parcelas**, que son las unidades físicas donde se instalan los sensores y se realizan las tareas de regeneración ambiental (compostaje, rotación de cultivos, etc.).

Actores que interactúan con este servicio:
- **Administradores de huertos** — registran y consultan parcelas.
- **Científicos ciudadanos** — consultan el estado del suelo por zona.
- **Líderes comunitarios** — supervisan la asignación de responsables.

---

## 2. Stack tecnológico

| Componente       | Tecnología                        |
|------------------|-----------------------------------|
| Lenguaje         | Java 17                           |
| Framework        | Spring Boot 3.2.5                 |
| API              | Spring Web (MVC)                  |
| Persistencia     | Spring Data JPA + Hibernate       |
| Base de datos    | PostgreSQL 15                     |
| Validación       | Jakarta Bean Validation 3         |
| Boilerplate      | Lombok                            |
| Contenedores     | Docker + Docker Compose           |
| Tests            | JUnit 5 + Spring MockMvc          |

---

## 3. Estructura del proyecto

```
parcel-service/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/urbangardens/parcelservice/
    │   │   ├── ParcelServiceApplication.java       # Entry point
    │   │   ├── controller/
    │   │   │   └── ParcelController.java           # Endpoints REST
    │   │   ├── dto/
    │   │   │   ├── ParcelRequestDTO.java           # Entrada (con validaciones)
    │   │   │   └── ParcelResponseDTO.java          # Salida (proyección limpia)
    │   │   ├── entity/
    │   │   │   ├── Parcel.java                     # Entidad JPA principal
    │   │   │   ├── GpsLocation.java                # Coordenadas GPS (@Embeddable)
    │   │   │   └── SoilType.java                   # Enum de tipos de suelo
    │   │   ├── exception/
    │   │   │   ├── ParcelNotFoundException.java     # 404
    │   │   │   ├── DuplicateParcelNameException.java # 409
    │   │   │   ├── InvalidParcelDataException.java  # 422
    │   │   │   ├── ErrorResponse.java              # Estructura uniforme de error
    │   │   │   └── GlobalExceptionHandler.java     # @RestControllerAdvice
    │   │   ├── repository/
    │   │   │   └── ParcelRepository.java           # Spring Data JPA
    │   │   ├── service/
    │   │   │   ├── ParcelService.java              # Interfaz del servicio
    │   │   │   └── ParcelServiceImpl.java          # Implementación
    │   │   └── util/
    │   │       └── SanitizationUtil.java           # Limpieza de inputs
    │   └── resources/
    │       └── application.yml                     # Configuración
    └── test/
        └── java/com/urbangardens/parcelservice/
            └── ParcelControllerTest.java           # Tests de capa web
```

---

## 4. Capas de la arquitectura

### 4.1 Entidades JPA

#### `Parcel.java`
Entidad principal mapeada a la tabla `parcels`. Representa una zona de cultivo física.

| Campo        | Tipo          | Descripción                                              |
|--------------|---------------|----------------------------------------------------------|
| `id`         | `UUID`        | Identificador generado automáticamente por la BD         |
| `name`       | `String`      | Nombre único de la parcela (máx. 120 caracteres)         |
| `location`   | `GpsLocation` | Coordenadas GPS embebidas (latitud + longitud)           |
| `soilType`   | `SoilType`    | Tipo de suelo (enum)                                     |
| `ownerOrLead`| `String`      | Nombre del responsable o líder de la parcela             |
| `createdAt`  | `LocalDateTime`| Generada automáticamente con `@CreationTimestamp`       |

La restricción `@UniqueConstraint` en `name` garantiza unicidad a nivel de base de datos como última línea de defensa.

#### `GpsLocation.java`
Objeto valor (`@Embeddable`) que almacena `latitude` y `longitude` como columnas dentro de la tabla `parcels` (no como tabla separada). Incluye sus propias restricciones `@DecimalMin` / `@DecimalMax`.

#### `SoilType.java`
Enum con los tipos de suelo reconocidos:
```
ARCILLOSO | LIMOSO | ARENOSO | FRANCO | FRANCO_ARCILLOSO | FRANCO_ARENOSO
```
Cada valor tiene un `displayName` en español para mostrarse al cliente.

---

### 4.2 DTOs

Se usan DTOs separados para entrada y salida — **la entidad JPA nunca se expone directamente** al cliente.

#### `ParcelRequestDTO.java`
DTO de entrada con todas las anotaciones de validación. Contiene un DTO anidado estático `GpsLocationDTO` para las coordenadas.

```java
// Ejemplo de las validaciones presentes en el DTO:
@NotBlank
@Size(min = 3, max = 120)
@Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-\\.,'#áéíóúÁÉÍÓÚñÑüÜ]+$")
private String name;

@NotNull @Valid
private GpsLocationDTO location;   // activa validaciones del objeto anidado

@NotNull
private SoilType soilType;

@NotBlank @Size(min = 3, max = 150)
@Pattern(regexp = "^[\\p{L}\\s\\-\\.áéíóúÁÉÍÓÚñÑüÜ]+$")
private String ownerOrLead;
```

#### `ParcelResponseDTO.java`
DTO de salida construido con el patrón Builder de Lombok. Incluye `soilTypeDisplayName` (nombre legible del enum) además del valor técnico.

---

### 4.3 Repositorio

#### `ParcelRepository.java`
Extiende `JpaRepository<Parcel, UUID>`. Spring Data genera la implementación en tiempo de ejecución.

Método personalizado:
```java
boolean existsByNameIgnoreCase(String name);
```
Permite verificar duplicados de nombre sin importar mayúsculas/minúsculas antes de intentar la inserción.

---

### 4.4 Servicio

#### `ParcelService.java`
Interfaz que define el contrato de la capa de negocio:
```java
ParcelResponseDTO createParcel(ParcelRequestDTO request);
List<ParcelResponseDTO> getAllParcels();
ParcelResponseDTO getParcelById(UUID id);
```

#### `ParcelServiceImpl.java`
Implementación con la lógica de negocio. El flujo de `createParcel` es:

```
1. sanitizeText(name)          → limpieza manual de XSS/SQL
2. sanitizeText(ownerOrLead)   → limpieza manual de XSS/SQL
3. validateOperationalCoordinates() → verifica rango geográfico
4. existsByNameIgnoreCase()    → verifica unicidad del nombre
5. parcelRepository.save()     → persiste en PostgreSQL
6. toResponseDTO()             → mapea entidad → DTO de salida
```

Las operaciones de sólo lectura (`getAllParcels`, `getParcelById`) usan `@Transactional(readOnly = true)` para optimizar el rendimiento.

---

### 4.5 Controlador REST

#### `ParcelController.java`
Expone los tres endpoints bajo la ruta base `/api/v1/parcels`. La anotación `@Valid` en el parámetro del `POST` activa Bean Validation antes de que el método se ejecute; cualquier violación es capturada automáticamente por el `GlobalExceptionHandler`.

---

## 5. Validación y sanitización

El sistema aplica **tres capas de defensa** sobre los datos de entrada:

### Capa 1 — Bean Validation (declarativa, en el DTO)
Se activa con `@Valid` en el controlador. Valida formato, tamaño y expresiones regulares antes de que los datos lleguen a la capa de servicio.

| Regla aplicada | Anotación | Ejemplo de rechazo |
|---|---|---|
| Campo obligatorio | `@NotBlank` / `@NotNull` | `name: ""` |
| Longitud | `@Size(min, max)` | `name` de 1 carácter |
| Rango numérico | `@DecimalMin` / `@DecimalMax` | `latitude: 999.0` |
| Formato de texto | `@Pattern(regexp)` | `name: "<script>alert(1)</script>"` |
| Objeto anidado | `@Valid` | Coordenadas GPS con campos nulos |

### Capa 2 — Sanitización manual (`SanitizationUtil`)
Se ejecuta en `ParcelServiceImpl` después de que el DTO pasa la validación declarativa. Es la segunda línea de defensa para valores que superan el regex pero podrían contener contenido malicioso.

```
1. Elimina bytes nulos (\0) y espacios extremos
2. Colapsa espacios internos múltiples
3. Elimina etiquetas HTML (<b>, <img>, etc.)
4. Detecta y rechaza eventos/código JavaScript (onclick=, javascript:, <script>)
5. Detecta y rechaza palabras reservadas SQL (SELECT, DROP, INSERT, etc.)
```

Si se detecta contenido malicioso en paso 4 o 5, se lanza `InvalidParcelDataException` con el nombre del campo afectado.

### Capa 3 — Validación de negocio (`validateOperationalCoordinates`)
Verifica que las coordenadas GPS estén dentro del área de operación de la red (Latinoamérica y España: lat [-60, 40], lon [-120, 10]). Evita registrar parcelas en coordenadas oceánicas o geográficamente imposibles para un huerto urbano.

---

## 6. Manejo de errores y excepciones personalizadas

### Excepciones de negocio

| Clase | Cuándo se lanza | HTTP |
|---|---|---|
| `ParcelNotFoundException` | No existe ninguna parcela con el UUID solicitado | `404` |
| `DuplicateParcelNameException` | Ya existe una parcela con el mismo nombre | `409` |
| `InvalidParcelDataException` | Datos lógicamente inválidos o contenido malicioso detectado | `422` |

Cada excepción almacena el dato relevante (`parcelId`, `duplicateName`, `field`) para incluirlo en el mensaje de error sin exponer trazas internas.

### `GlobalExceptionHandler.java`
Clase anotada con `@RestControllerAdvice` que centraliza el manejo de todas las excepciones. Convierte cualquier excepción en un `ErrorResponse` con estructura uniforme.

**Handlers registrados:**

| Excepción | HTTP | Código de error |
|---|---|---|
| `ParcelNotFoundException` | `404` | `PARCELA_NO_ENCONTRADA` |
| `DuplicateParcelNameException` | `409` | `NOMBRE_DUPLICADO` |
| `InvalidParcelDataException` | `422` | `DATO_INVALIDO` |
| `MethodArgumentNotValidException` | `400` | `SOLICITUD_INVALIDA` |
| `HttpMessageNotReadableException` | `400` | `CUERPO_ILEGIBLE` |
| `MethodArgumentTypeMismatchException` | `400` | `PARAMETRO_INVALIDO` |
| `NoHandlerFoundException` | `404` | `RUTA_NO_ENCONTRADA` |
| `Exception` (catch-all) | `500` | `ERROR_INTERNO` |

### `ErrorResponse.java`
Estructura uniforme de error devuelta al cliente en todos los casos de error:

```json
{
  "status": 400,
  "error": "SOLICITUD_INVALIDA",
  "message": "La solicitud contiene 2 error(es) de validación. Revise los campos indicados.",
  "path": "/api/v1/parcels",
  "timestamp": "2026-10-14T10:30:00",
  "errors": [
    {
      "field": "name",
      "rejectedValue": "",
      "message": "El nombre de la parcela es obligatorio y no puede estar vacío"
    },
    {
      "field": "location.latitude",
      "rejectedValue": 999.0,
      "message": "La latitud no puede ser mayor a 90.0"
    }
  ]
}
```

El campo `errors` sólo aparece en respuestas `400` con errores de validación por campo. Los demás campos opcionales se omiten con `@JsonInclude(NON_NULL)`.

---

## 7. API Reference

### `POST /api/v1/parcels`
Registra una nueva parcela.

**Request body:**
```json
{
  "name": "Huerto Norte - Bloque A",
  "location": {
    "latitude": 19.432608,
    "longitude": -99.133209
  },
  "soilType": "ARCILLOSO",
  "ownerOrLead": "María López"
}
```

**Respuestas:**

| Código | Descripción |
|--------|-------------|
| `201 Created` | Parcela creada exitosamente |
| `400 Bad Request` | Campos inválidos o JSON malformado |
| `409 Conflict` | Ya existe una parcela con ese nombre |
| `422 Unprocessable Entity` | Datos lógicamente incorrectos (coordenadas fuera de rango operativo, contenido malicioso) |

---

### `GET /api/v1/parcels`
Retorna todas las parcelas registradas.

**Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200 OK` | Lista de parcelas (puede ser `[]`) |

---

### `GET /api/v1/parcels/{id}`
Retorna el detalle de una parcela por su UUID.

**Path parameter:** `id` — UUID v4 de la parcela.

**Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200 OK` | Datos de la parcela |
| `400 Bad Request` | El `id` no tiene formato UUID válido |
| `404 Not Found` | No existe ninguna parcela con ese ID |

**Response body (ejemplo):**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "Huerto Norte - Bloque A",
  "location": {
    "latitude": 19.432608,
    "longitude": -99.133209
  },
  "soilType": "ARCILLOSO",
  "soilTypeDisplayName": "Arcilloso",
  "ownerOrLead": "María López",
  "createdAt": "2026-10-14T10:00:00"
}
```

---

## 8. Ejecutar el proyecto

### Prerrequisitos
- Docker y Docker Compose instalados
- Java 17+ y Maven (sólo si se ejecuta sin Docker)

### Con Docker Compose (recomendado)
```bash
# Desde la raíz del proyecto
docker-compose up -d
```
Levanta PostgreSQL en el puerto `5432` y el servicio en el puerto `8081`.

### Sin Docker (desarrollo local)
```bash
# 1. Levantar sólo la base de datos
docker-compose up -d postgres

# 2. Ejecutar el servicio con Maven
mvn spring-boot:run
```

El servicio queda disponible en: `http://localhost:8081/api/v1/parcels`

### Variables de entorno configurables

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/urban_gardens` | URL de conexión a PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `urban_user` | Usuario de la base de datos |
| `SPRING_DATASOURCE_PASSWORD` | `urban_pass` | Contraseña de la base de datos |
| `SERVER_PORT` | `8081` | Puerto del servidor HTTP |

---

## 9. Tests

El archivo `ParcelControllerTest.java` contiene tests de capa web con `@WebMvcTest` (no requieren base de datos, usan MockMvc + Mockito).

**Casos cubiertos:**

| Test | Escenario | Código esperado |
|---|---|---|
| `createParcel_emptyName_returns400` | Nombre vacío | `400` + campo `name` en `errors` |
| `createParcel_latitudeOutOfRange_returns400` | Latitud `999.0` | `400` + campo `location.latitude` |
| `createParcel_invalidSoilType_returns400` | Enum inexistente | `400` + `CUERPO_ILEGIBLE` |
| `createParcel_duplicateName_returns409` | Nombre duplicado | `409` + `NOMBRE_DUPLICADO` |
| `getParcelById_notFound_returns404` | UUID inexistente | `404` + `PARCELA_NO_ENCONTRADA` |
| `getParcelById_malformedUuid_returns400` | ID no UUID | `400` + `PARAMETRO_INVALIDO` |

```bash
# Ejecutar todos los tests
mvn test
```
