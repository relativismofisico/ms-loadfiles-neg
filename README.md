# ms-loadfiles-neg

Microservicio de carga y gestión de archivos para el sistema **Karonte Factoring**.

---

## Tabla de Contenidos

1. [Descripción General](#1-descripción-general)
2. [Tecnologías](#2-tecnologías)
3. [Requisitos Previos](#3-requisitos-previos)
4. [Configuración](#4-configuración)
5. [Ejecución Local](#5-ejecución-local)
6. [Swagger / OpenAPI](#6-swagger--openapi)
7. [Endpoints](#7-endpoints)
8. [Reglas de Negocio](#8-reglas-de-negocio)
9. [Modelo de Datos](#9-modelo-de-datos)
10. [Seguridad](#10-seguridad)
11. [Observabilidad](#11-observabilidad)
12. [Pruebas](#12-pruebas)
13. [Estructura del Proyecto](#13-estructura-del-proyecto)
14. [Diagramas](#14-diagramas)
15. [Ambientes](#15-ambientes)
16. [Troubleshooting](#16-troubleshooting)
17. [Consideraciones Técnicas](#17-consideraciones-técnicas)
18. [Changelog](#18-changelog)

---

## 1. Descripción General

### Objetivo del microservicio

`ms-loadfiles-neg` gestiona el ciclo de vida completo de archivos en el sistema Karonte Factoring: carga de documentos de empresa, flujo de firma digital (archivos pendientes y firmados) y almacenamiento de imágenes de firma digital.

### Problema de negocio que resuelve

En una operación de factoring, las empresas deben cargar y mantener actualizados documentos corporativos (escrituras, poderes, registros). Adicionalmente, ciertos documentos requieren firma digital por parte de operarios autorizados antes de quedar vigentes. Este microservicio centraliza todas estas operaciones de archivo.

### Alcance funcional

| Módulo | Descripción |
|--------|-------------|
| **Archivos de empresa** | Carga, reemplazo y consulta de documentos PDF de empresa por tipo |
| **Archivos para firmar** | Listado y descarga de documentos pendientes de firma (tipo registro, `companyFileType = 10`) |
| **Archivos firmados** | Carga y consulta de documentos PDF que han sido firmados |
| **Firmas digitales** | Guardado, reemplazo y consulta de imágenes de firma por empresa y por usuario |

### Arquitectura utilizada

Arquitectura hexagonal (ports & adapters) con cuatro capas explícitas:

- **`domain`** — Entidades JPA puras, sin dependencias de Spring
- **`application`** — Interfaces de servicio (ports), excepciones de dominio, constantes
- **`infraestrutura`** — Implementaciones (adapters): use cases, repositorios JPA, seguridad, configuración
- **`web`** — Controladores REST, DTOs, manejador global de excepciones

---

## 2. Tecnologías

| Tecnología | Versión | Uso |
|-----------|---------|-----|
| **Java** | 17 | Lenguaje principal |
| **Spring Boot** | 3.3.4 | Framework base |
| **Spring Data JPA** | (incluido en Boot 3.3.4) | Persistencia ORM |
| **Spring Security** | 6.x (incluido en Boot 3.3.4) | Seguridad y autenticación |
| **Spring Boot Actuator** | (incluido en Boot 3.3.4) | Health checks |
| **Gradle** | 8.14 | Herramienta de construcción |
| **MySQL** | Compatible 8.x | Base de datos principal (AWS RDS) |
| **SQL Server** | (driver incluido) | Driver alternativo disponible |
| **springdoc-openapi** | 2.6.0 | Documentación OpenAPI 3 / Swagger UI |
| **jjwt** | 0.12.3 | Validación de tokens JWT |
| **ModelMapper** | 2.4.4 | Conversión entre entidades y DTOs |
| **Gson** | 2.10.1 | Serialización JSON auxiliar |
| **Lombok** | (gestionado por BOM de Spring Boot) | Reducción de boilerplate |
| **Spock Framework** | 2.3-groovy-4.0 | Pruebas unitarias (Groovy) |
| **ArchUnit** | 1.3.0 | Pruebas de arquitectura |
| **Karate** | 1.4.0 | Pruebas funcionales/aceptación |
| **JaCoCo** | 0.8.11 | Cobertura de código |
| **Checkstyle** | 10.17.0 | Análisis de estilo de código |
| **PMD** | 7.3.0 | Análisis estático de código |
| **SpotBugs** | 4.8.6 | Detección de bugs estáticos |
| **SonarQube plugin** | 5.1.0.4882 | Integración con SonarQube |
| **JMeter** | 5.6+ | Pruebas de rendimiento |
| **Docker** | — | Contenedorización |
| **Eclipse Temurin** | 17-alpine | Imagen base Docker |

---

## 3. Requisitos Previos

| Requisito | Versión mínima | Notas |
|----------|---------------|-------|
| **JDK** | 17 | Temurin / OpenJDK 17+ |
| **Gradle** | 8.5+ | Incluido en el wrapper (`./gradlew`) |
| **MySQL** | 8.x | Instancia AWS RDS en DEV (credenciales en `application-dev.yml`) |
| **Docker** | 20+ | Solo para despliegue en contenedor |
| **JMeter** | 5.6+ | Solo para pruebas de rendimiento externas |

### Dependencias externas

- **Microservicio de seguridad (`ms-security`)**: El token JWT es emitido por un microservicio externo. Este servicio solo lo valida.
- **Base de datos `dbcompany`**: MySQL en AWS RDS. El esquema debe existir previamente (tablas `files`, `files_signed`, `files_to_sign`, `file_type`, `sign`).
- **Sistema de archivos local**: El servicio almacena archivos en el sistema de archivos del servidor en las carpetas `uploadfiles/`, `files_registry_signed/`, `files_to_sign/company_registry/` y `signatures/`. En producción estas rutas deben estar en un volumen persistente.

---

## 4. Configuración

### Variables de entorno (QA y Producción)

| Variable | Descripción | Requerida en |
|----------|-------------|-------------|
| `DB_HOST` | Host de la base de datos MySQL | QA, PROD |
| `DB_NAME` | Nombre de la base de datos | QA, PROD |
| `DB_USERNAME` | Usuario de la base de datos | QA, PROD |
| `DB_PASSWORD` | Contraseña de la base de datos | QA, PROD |
| `JWT_SECRET` | Clave secreta Base64 para validación JWT | QA, PROD |

> **IMPORTANTE:** En el ambiente `dev`, las credenciales de BD y el JWT secret están hardcodeados en `application-dev.yml`. Esto es solo para desarrollo local y **NUNCA debe usarse en ambientes superiores**.

### Archivo `application.yml` (base)

```yaml
spring:
  application:
    name: ms-security            # Nota: pendiente corregir a ms-loadfiles-neg
  profiles:
    active: dev
  datasource:
    url: "jdbc:mysql://dbcompany.cn48sgciaax1.us-east-2.rds.amazonaws.com:3306/dbcompany?useSSL=false&serverTimezone=UTC"
    username: desarrollador
    password: "Juliana1987****"
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 5000
      idle-timeout: 300000
      max-lifetime: 600000
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: 8081

management:
  server:
    port: 8082
  endpoints:
    web:
      exposure:
        include: health

security:
  jwt:
    secret: AF84F1FGllNpNnLG055fdg5hGHJK4KGG5VH5TR5J05JFGGDFDGXVV545J4505G666JFGF2mMY95y
  rol:
    todos: [ADMINISTRADOR, EMPRESA, OPERARIO, FONDEADOR]
    carga: [ADMINISTRADOR, EMPRESA, OPERARIO]
    revision: [ADMINISTRADOR, OPERARIO, FONDEADOR]
    firma-subida: [ADMINISTRADOR, OPERARIO]
    firma-guardado: [ADMINISTRADOR, EMPRESA]
```

### Configuración por ambiente

| Parámetro | DEV | QA | PROD |
|-----------|-----|----|------|
| `ddl-auto` | `update` | `validate` | `none` |
| `show-sql` | `true` | `false` | `false` |
| `maximum-pool-size` | 10 (default) | 10 (default) | 20 |
| Swagger habilitado | Sí | Sí | **No** |
| JWT secret | Hardcodeado | Variable `${JWT_SECRET}` | Variable `${JWT_SECRET}` |
| SSL BD | `false` | `true` | `true` (`requireSSL=true`) |
| Log level `com.loadfilesservice` | `debug` | `info` | `warn` |

---

## 5. Ejecución Local

### Compilar el proyecto

```bash
./gradlew build
```

### Compilar sin ejecutar pruebas

```bash
./gradlew build -x test
```

### Ejecutar la aplicación

```bash
./gradlew bootRun
```

La aplicación levanta en `http://localhost:8081` (perfil `dev` por defecto).

### Ejecutar pruebas unitarias (Spock)

```bash
./gradlew test
```

### Ejecutar pruebas de arquitectura (ArchUnit)

```bash
./gradlew architectureTest
```

### Ejecutar pruebas funcionales (Karate)

```bash
# Configurar primero karate.properties (ver sección 12)
./gradlew automatedTest
```

### Generar reporte de cobertura JaCoCo

```bash
./gradlew jacocoTestReport
# Reporte HTML: build/jacocoHtml/index.html
```

### Verificar umbral de cobertura

```bash
./gradlew jacocoTestCoverageVerification
# Mínimo: 85% ramas, 80% líneas
```

### Análisis de calidad (Checkstyle + PMD + SpotBugs)

```bash
./gradlew check
```

### Publicar en SonarQube

```bash
./gradlew sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<token>
```

### Construir imagen Docker

```bash
docker build -t ms-loadfiles-neg:1.0.0 .
```

### Ejecutar contenedor Docker

```bash
docker run -p 8081:8080 \
  -e DB_HOST=<host> \
  -e DB_NAME=dbcompany \
  -e DB_USERNAME=<user> \
  -e DB_PASSWORD=<pass> \
  -e JWT_SECRET=<secret> \
  -e SPRING_PROFILES_ACTIVE=qa \
  ms-loadfiles-neg:1.0.0
```

> **Nota:** El Dockerfile expone el puerto `8080`. La aplicación internamente corre en `8081` con el perfil dev. En contenedor, configurar el puerto vía `server.port` o variables de entorno según el ambiente.

---

## 6. Swagger / OpenAPI

### Habilitar Swagger

Swagger está habilitado por defecto en los perfiles `dev` y `qa`. En producción está **deshabilitado** (`springdoc.api-docs.enabled: false`).

### URLs

| Recurso | URL |
|---------|-----|
| Swagger UI | `http://localhost:8081/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8081/v3/api-docs` |

### Autenticación requerida en Swagger

Todos los endpoints (excepto `/actuator/health/**`) requieren autenticación JWT. Para probar desde Swagger:

1. Obtener un token JWT desde el microservicio `ms-security`.
2. En Swagger UI, hacer clic en el botón **"Authorize"** (candado).
3. En el campo `bearerAuth`, ingresar el token sin el prefijo `Bearer `:
   ```
   eyJhbGciOiJIUzI1NiJ9...
   ```
4. Hacer clic en **"Authorize"** y cerrar el diálogo.
5. Ahora todos los endpoints enviarán el header `Authorization: Bearer <token>` automáticamente.

### Cómo probar endpoints desde Swagger

- Para endpoints que reciben `multipart/form-data` (upload de archivos), Swagger UI muestra campos de formulario donde se puede seleccionar el archivo y los datos JSON.
- Para endpoints que reciben `@RequestBody` con JSON, se puede editar el body directamente en el editor de Swagger.
- Los endpoints que retornan PDF (`application/pdf`) no se pueden previsualizar en Swagger; descargar la respuesta como archivo binario.

---

## 7. Endpoints

> **Header requerido en todos los endpoints (excepto health y swagger):**
> ```
> Authorization: Bearer <token-jwt>
> ```
>
> **Contexto de la aplicación:** `/load` (según configuración de proxy/gateway en QA y PROD). En desarrollo local: sin contexto de ruta.

---

### 7.1 Archivos de Empresa (`/api`)

#### POST `/api/companyfile/upload`

Sube un archivo PDF de empresa y lo registra en base de datos. Si ya existe un archivo activo del mismo tipo para la empresa, lo desactiva y lo elimina del sistema de archivos.

- **Roles requeridos:** `carga` → ADMINISTRADOR, EMPRESA, OPERARIO
- **Content-Type:** `multipart/form-data`
- **Parámetros de formulario:**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `file` | `file` (PDF) | Archivo PDF a subir |
| `fileinfo` | `string` (JSON) | `{"ipLoad":"192.168.1.1","company":1,"companyFileType":{"id":10,"description":"Registro","fileTypeName":"REGISTRO"}}` |

- **Respuestas:**

| Código | Descripción | Body |
|--------|-------------|------|
| `201` | Archivo guardado | `{"savedFile": {...CompanyFile}}` |
| `400` | JSON de `fileinfo` inválido | `ApiErrorResponse` |
| `401` | Token ausente o inválido | `ApiErrorResponse` |
| `403` | Rol insuficiente | `ApiErrorResponse` |
| `500` | Error al guardar en disco o BD | `ApiErrorResponse` |

---

#### POST `/api/companyfile/upload/file/{id}`

Retorna el nombre del archivo activo de una empresa filtrado por tipo.

- **Roles requeridos:** `todos` → ADMINISTRADOR, EMPRESA, OPERARIO, FONDEADOR
- **Path variable:** `id` (Long) — ID de la empresa
- **Request body (JSON):** `CompanyFileType` → `{"id":10,"fileTypeName":"REGISTRO","description":"Registro"}`

- **Respuestas:**

| Código | Descripción | Body |
|--------|-------------|------|
| `200` | OK (puede estar vacío si no hay activo) | `{"fileName": "uuid_nombre.pdf"}` o `{}` |
| `401` | Token ausente o inválido | `ApiErrorResponse` |
| `403` | Rol insuficiente | `ApiErrorResponse` |
| `500` | Error al consultar BD | `ApiErrorResponse` |

---

#### GET `/api/listofcompanyfiles/{id}`

Retorna todos los archivos activos (estado = 1) de una empresa.

- **Roles requeridos:** `todos`
- **Path variable:** `id` (Long) — ID de la empresa

- **Respuestas:**

| Código | Descripción | Body |
|--------|-------------|------|
| `200` | Lista retornada | `[CompanyFileDTOResponse]` |
| `401` | Token ausente o inválido | `ApiErrorResponse` |
| `403` | Rol insuficiente | `ApiErrorResponse` |
| `404` | No hay archivos activos para la empresa | `ApiErrorResponse` |

---

#### POST `/api/companyfilepdf`

Retorna el contenido binario (bytes) de un archivo PDF de empresa.

- **Roles requeridos:** `todos`
- **Request body (JSON):** `{"id": 1}` (CompanyFile con ID)
- **Response Content-Type:** `application/pdf`

- **Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200` | PDF en bytes |
| `401` | Token ausente o inválido |
| `403` | Rol insuficiente |
| `500` | Archivo no encontrado en disco o BD |

---

### 7.2 Archivos para Firmar (`/filesign`)

#### GET `/filesign/filestosignregistry`

Lista los archivos de tipo registro de empresa (`companyFileType = 10`) con estado activo (estado = 1) pendientes de firma.

- **Roles requeridos:** `revision` → ADMINISTRADOR, OPERARIO, FONDEADOR

- **Respuestas:**

| Código | Descripción | Body |
|--------|-------------|------|
| `200` | Lista retornada | `[FileToSignDTOResponse]` |
| `401` | Token ausente o inválido | `ApiErrorResponse` |
| `403` | Rol insuficiente | `ApiErrorResponse` |
| `404` | No hay archivos pendientes | `ApiErrorResponse` |

---

#### POST `/filesign/pdftosign`

Retorna el contenido binario (bytes) de un archivo pendiente de firma.

- **Roles requeridos:** `revision`
- **Request body (JSON):** `{"id": 1}` (FileToSign con ID)
- **Response Content-Type:** `application/pdf`

- **Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200` | PDF en bytes |
| `401` | Token ausente o inválido |
| `403` | Rol insuficiente |
| `500` | Error al acceder al archivo |

---

#### POST `/filesign/uploadsignedpdf`

Sube un PDF firmado y lo registra en la base de datos. Crea automáticamente la carpeta `files_registry_signed/{companyName}/` si no existe.

- **Roles requeridos:** `firma-subida` → ADMINISTRADOR, OPERARIO
- **Content-Type:** `multipart/form-data`
- **Parámetros de formulario:**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `file` | `file` (PDF) | PDF firmado |
| `fileinfo` | `string` (JSON) | `{"originalFileName":"doc.pdf","ipLoad":"192.168.1.1","companyName":"EMPRESA_SA","company":1,"user":5,"companyFileType":{"id":10}}` |

- **Respuestas:**

| Código | Descripción | Body |
|--------|-------------|------|
| `201` | Guardado exitoso | `{"saveFile": true}` |
| `400` | Archivo vacío o JSON inválido | `ApiErrorResponse` |
| `401` | Token ausente o inválido | `ApiErrorResponse` |
| `403` | Rol insuficiente | `ApiErrorResponse` |
| `500` | Error interno | `ApiErrorResponse` |

---

### 7.3 Archivos Firmados (`/filessigned`)

#### GET `/filessigned/listofcompanysignedfiles/{id}`

Retorna todos los archivos firmados activos (estado = 1) de una empresa.

- **Roles requeridos:** `todos`
- **Path variable:** `id` (Long) — ID de la empresa

- **Respuestas:**

| Código | Descripción | Body |
|--------|-------------|------|
| `200` | Lista retornada | `[FileSigned]` |
| `401` | Token ausente o inválido | `ApiErrorResponse` |
| `403` | Rol insuficiente | `ApiErrorResponse` |
| `404` | No hay archivos firmados para la empresa | `ApiErrorResponse` |

---

#### POST `/filessigned/companyfilesignedpdf/{companyName}`

Retorna el contenido binario (bytes) de un archivo firmado.

- **Roles requeridos:** `todos`
- **Path variable:** `companyName` (String) — Identificador de la empresa (ej. `EMPRESA_SA`)
- **Request body (JSON):** `{"id": 1}` (FileSigned con ID)
- **Response Content-Type:** `application/pdf`

- **Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200` | PDF firmado en bytes |
| `401` | Token ausente o inválido |
| `403` | Rol insuficiente |
| `500` | Error al acceder al archivo |

---

### 7.4 Firmas Digitales (`/signs`)

#### POST `/signs/save`

Guarda o reemplaza la firma activa de una empresa. Si ya existe una firma activa para la empresa, la desactiva y elimina el archivo físico anterior.

- **Roles requeridos:** `firma-guardado` → ADMINISTRADOR, EMPRESA
- **Content-Type:** `multipart/form-data`
- **Parámetros de formulario:**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `file` | `file` (PNG/JPG) | Imagen de la firma digital |
| `company` | `Long` | ID de la empresa |
| `user` | `Long` | ID del usuario (0 = firma de empresa) |
| `ipLoad` | `String` | Dirección IP de origen |
| `companyName` | `String` | Nombre identificador de la empresa para la ruta |

- **Respuestas:**

| Código | Descripción | Body |
|--------|-------------|------|
| `201` | Firma guardada | `Sign` completo |
| `401` | Token ausente o inválido | `ApiErrorResponse` |
| `403` | Rol insuficiente | `ApiErrorResponse` |
| `500` | Error interno | `ApiErrorResponse` |

---

#### GET `/signs/company/{companyId}`

Retorna el archivo de imagen de la firma activa (estado = 1) de una empresa.

- **Roles requeridos:** `todos`
- **Path variable:** `companyId` (Long)
- **Response Content-Type:** `application/octet-stream`

- **Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200` | Imagen de firma como stream binario |
| `404` | No hay firma activa para la empresa |
| `401` | Token ausente o inválido |
| `403` | Rol insuficiente |
| `500` | Error al cargar el archivo |

---

#### GET `/signs/user/{userId}`

Retorna el archivo de imagen de la firma activa (estado = 1) de un usuario.

- **Roles requeridos:** `todos`
- **Path variable:** `userId` (Long)
- **Response Content-Type:** `application/octet-stream`

- **Respuestas:**

| Código | Descripción |
|--------|-------------|
| `200` | Imagen de firma como stream binario |
| `404` | No hay firma activa para el usuario |
| `401` | Token ausente o inválido |
| `403` | Rol insuficiente |
| `500` | Error al cargar el archivo |

---

### Estructura del objeto `ApiErrorResponse`

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Autenticación requerida: token JWT ausente o inválido",
  "path": "/api/listofcompanyfiles/1"
}
```

---

## 8. Reglas de Negocio

### Archivos de Empresa

- Solo puede haber **un archivo activo** (`state = 1`) por empresa y por tipo de archivo (`companyFileType`).
- Al subir un nuevo archivo de un tipo ya existente, el archivo activo previo es **desactivado** (`state = 0`) y **eliminado del sistema de archivos** antes de guardar el nuevo.
- Los archivos son renombrados al momento de guardarse con formato `UUID_nombreoriginal.pdf` para evitar colisiones.
- Los espacios en el nombre original del archivo son eliminados antes de guardar.
- El nombre original del archivo se preserva en el campo `originalFileName` para mostrarlo al usuario.
- Los listados de archivos de empresa retornan únicamente los de estado = 1 (activos).
- Si la empresa no tiene archivos activos, se retorna `404 Not Found`.
- El campo `state` se gestiona internamente y no es enviado por el cliente.
- El campo `loadTime` es asignado automáticamente por el servicio con la fecha/hora actual.
- El campo `filePath` es asignado automáticamente con la ruta absoluta calculada.

### Archivos para Firmar

- Solo se listan archivos de tipo registro de empresa (`companyFileType.id = 10`) con `state = 1`.
- Si no hay archivos pendientes de firma, se retorna `404 Not Found`.
- Los archivos pendientes de firma se ubican en la ruta: `files_to_sign/company_registry/`.

### Archivos Firmados

- Los archivos firmados se almacenan en carpetas separadas por empresa: `files_registry_signed/{companyName}/`.
- La carpeta es creada automáticamente si no existe al momento de la primera subida.
- El estado inicial de un archivo firmado al guardarse es siempre `1` (activo).
- Si la empresa no tiene archivos firmados activos, se retorna `404 Not Found`.

### Firmas Digitales

- Solo puede haber **una firma activa** (`state = 1`) por empresa.
- Al guardar una nueva firma para una empresa que ya tiene firma activa, la firma anterior es **desactivada** (`state = 0`) y el archivo físico es **eliminado** del servidor.
- Si la eliminación del archivo físico anterior falla, se registra un `WARN` en logs pero el proceso continúa (la nueva firma se guarda igualmente).
- Si el parámetro `user = 0`, el campo `user` se almacena como `null` en base de datos (es una firma de empresa, no de usuario).
- Las firmas se almacenan en: `signatures/{companyName}/`.
- La carpeta es creada automáticamente si no existe.
- El campo `loadTime` es asignado automáticamente.

### Validaciones transversales

- Si el archivo subido (`MultipartFile`) está vacío, se retorna `400 Bad Request`.
- Si el JSON del campo `fileinfo` no puede deserializarse, se retorna `400 Bad Request`.
- Si un recurso solicitado no existe en BD, se retorna `500 Internal Server Error` (en los casos donde se accede por ID directo) o `404 Not Found` (en los listados).
- El tamaño máximo de archivo aceptado es **10 MB** (multipart).

---

## 9. Modelo de Datos

### Tablas SQL (MySQL)

#### `files` → Entidad `CompanyFile`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `ide_file` | BIGINT PK AUTO_INCREMENT | Identificador único |
| `file_name` | VARCHAR | Nombre UUID generado al guardar |
| `original_file_name` | VARCHAR | Nombre original del archivo |
| `file_path` | VARCHAR | Ruta absoluta en el sistema de archivos |
| `ip_load` | VARCHAR NOT NULL | IP de origen de la carga |
| `load_time` | DATETIME | Fecha/hora de carga (asignada por servicio) |
| `company_ide` | BIGINT | ID de la empresa |
| `state` | BIGINT | `1` = activo, `0` = inactivo |
| `file_type_ide` | BIGINT FK | Referencia a `file_type.ide_file_type` |

#### `files_signed` → Entidad `FileSigned`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | BIGINT PK AUTO_INCREMENT | Identificador único |
| `file_name` | VARCHAR | Nombre UUID del archivo firmado |
| `original_file_name` | VARCHAR | Nombre original |
| `file_path` | VARCHAR | Ruta relativa (ej. `files_registry_signed/EMPRESA_SA`) |
| `ip_load` | VARCHAR | IP de origen |
| `load_time` | DATETIME | Fecha/hora de carga |
| `state` | BIGINT | `1` = activo, `0` = inactivo |
| `company_ide` | BIGINT | ID de la empresa |
| `user_ide` | BIGINT | ID del usuario que firmó |
| `file_type_ide` | BIGINT FK | Referencia a `file_type.ide_file_type` |

#### `files_to_sign` → Entidad `FileToSign`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | BIGINT PK AUTO_INCREMENT | Identificador único |
| `file_name` | VARCHAR | Nombre del archivo pendiente de firma |
| `file_path` | VARCHAR | Ruta del archivo |
| `ip_load` | VARCHAR | IP de origen |
| `load_time` | VARCHAR | Fecha/hora de carga (como String) |
| `state` | BIGINT | `1` = pendiente, `0` = procesado |
| `file_type_ide` | BIGINT FK | Referencia a `file_type.ide_file_type` |

#### `file_type` → Entidad `CompanyFileType`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `ide_file_type` | BIGINT PK AUTO_INCREMENT | Identificador único |
| `description` | VARCHAR | Descripción larga del tipo |
| `file_type_name` | VARCHAR | Nombre corto del tipo (ej. `REGISTRO`) |

#### `sign` → Entidad `Sign`

| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | BIGINT PK AUTO_INCREMENT | Identificador único |
| `file_name` | VARCHAR | Nombre UUID del archivo de firma |
| `file_path` | VARCHAR | Ruta relativa (ej. `signatures/EMPRESA_SA`) |
| `ip_load` | VARCHAR | IP de origen |
| `load_time` | DATETIME | Fecha/hora de carga |
| `state` | BIGINT | `1` = activo, `0` = inactivo |
| `company_ide` | BIGINT | ID de la empresa |
| `user_ide` | BIGINT | ID del usuario (nullable) |

### Relaciones

```
CompanyFile    ──── ManyToOne ──→ CompanyFileType
FileSigned     ──── ManyToOne ──→ CompanyFileType
FileToSign     ──── ManyToOne ──→ CompanyFileType
Sign           ──── (sin relación FK directa con entidades del dominio)
```

### Consultas JPQL personalizadas

```sql
-- Archivos por empresa y tipo
SELECT f FROM CompanyFile f WHERE f.company=?1 AND f.companyFileType=?2

-- Archivos activos con JOIN FETCH (evita N+1)
SELECT f FROM CompanyFile f JOIN FETCH f.companyFileType WHERE f.company=?1 AND f.state=?2

-- Firma activa por empresa
SELECT s FROM Sign s WHERE s.company=:company AND s.state=1

-- Firma activa por usuario
SELECT s FROM Sign s WHERE s.user=:user AND s.state=1
```

---

## 10. Seguridad

### Mecanismo de autenticación

- **Tipo:** JWT Bearer (stateless)
- **Librería:** `io.jsonwebtoken:jjwt` versión `0.12.3`
- **Algoritmo:** HMAC-SHA (clave derivada del secreto Base64 configurado en `security.jwt.secret`)
- **Flujo:** El token es emitido por el microservicio `ms-security`. Este servicio solo lo **valida**.

### Header requerido

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Endpoints públicos (sin autenticación)

```
GET  /actuator/health
GET  /actuator/health/liveness
GET  /actuator/health/readiness
GET  /swagger-ui/**
GET  /swagger-ui.html
GET  /v3/api-docs/**
GET  /v3/api-docs
```

### Filtro JWT (`JwtAuthenticationFilter`)

1. Extrae el token del header `Authorization: Bearer <token>`.
2. Valida la firma, expiración y formato usando `JwtTokenValidator`.
3. Extrae `username` y `rol` de los claims del token.
4. Registra la autenticación en el `SecurityContextHolder`.

### Claims del token JWT

| Claim | Descripción |
|-------|-------------|
| `sub` | Nombre de usuario |
| `rol` | Rol del usuario (ej. `ADMINISTRADOR`, `EMPRESA`) |

### Grupos de roles (definidos en `application.yml`)

| Grupo | Roles permitidos | Endpoints que lo usan |
|-------|-----------------|----------------------|
| `todos` | ADMINISTRADOR, EMPRESA, OPERARIO, FONDEADOR | Listados y PDFs de consulta |
| `carga` | ADMINISTRADOR, EMPRESA, OPERARIO | Upload de archivos de empresa |
| `revision` | ADMINISTRADOR, OPERARIO, FONDEADOR | Archivos pendientes de firma |
| `firma-subida` | ADMINISTRADOR, OPERARIO | Upload de PDFs firmados |
| `firma-guardado` | ADMINISTRADOR, EMPRESA | Guardar imágenes de firma |

### Validación de roles

Los roles se validan mediante el bean `RolValidator` (`@rolValidator`) usando expresiones SpEL en `@PreAuthorize`:

```java
@PreAuthorize("@rolValidator.hasRol(authentication, 'carga')")
```

El validador compara las authorities del token con la lista del grupo indicado en `security.rol.<grupo>` del YAML.

### Manejo de errores de seguridad

| Escenario | Respuesta |
|-----------|-----------|
| Token ausente o inválido | `401 Unauthorized` con `ApiErrorResponse` |
| Token expirado | `401 Unauthorized` |
| Rol insuficiente | `403 Forbidden` con `ApiErrorResponse` |

### Sesión

No se crean sesiones HTTP (`SessionCreationPolicy.STATELESS`). CSRF deshabilitado.

### CORS

| Controlador | Orígenes permitidos |
|-------------|---------------------|
| `LoadFilesRestController` | `http://localhost:4300`, `http://localhost:4600` |
| `FilesToSignRestController` | `http://localhost:4300`, `http://localhost:4400` |
| `FilesSignedRestController` | `http://localhost:4300`, `http://localhost:4600` |
| `SignController` | `http://localhost:4300`, `http://localhost:4400` |

> **Pendiente por definir:** Los orígenes CORS para ambientes QA y producción (actualmente solo están configurados para desarrollo local).

---

## 11. Observabilidad

### Spring Boot Actuator

El actuator corre en un **puerto separado** (`8082`) para no exponer métricas en el mismo puerto de la API.

- **Puerto de management:** `8082`
- **Endpoint expuesto:** `health`
- **URL health:** `http://localhost:8082/actuator/health`

### Health Checks y Probes

El microservicio expone probes de Kubernetes:

| Probe | URL | Descripción |
|-------|-----|-------------|
| Liveness | `http://localhost:8082/actuator/health/liveness` | Indica si el proceso está vivo |
| Readiness | `http://localhost:8082/actuator/health/readiness` | Indica si puede recibir tráfico |

Configuración en `application.yml`:

```yaml
management:
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

### Logging

El microservicio usa **SLF4J con Logback** (incluido por Spring Boot). Formato de logs estructurado con contexto:

```
[ClassName][methodName][loadfiles] Mensaje descriptivo
```

| Ambiente | Nivel `com.loadfilesservice` | Nivel `org.hibernate.SQL` | Nivel `org.springframework.security` |
|---------|------------------------------|--------------------------|--------------------------------------|
| DEV | DEBUG | DEBUG | DEBUG |
| QA | INFO | WARN | INFO |
| PROD | WARN | ERROR | WARN |

### Métricas

Las métricas de Micrometer/Actuator no están habilitadas en la configuración actual (solo se expone `health`).

### Trazabilidad

No hay configuración de tracing distribuido (OpenTelemetry/Zipkin) implementada actualmente.

---

## 12. Pruebas

### 12.1 Pruebas Unitarias (Spock Framework)

**Framework:** Spock 2.3-groovy-4.0 con JUnit 5  
**Comando:** `./gradlew test`  
**Cobertura alcanzada:** ~99% líneas / ~97% ramas

**Clases de prueba:**

| Spec | Cobertura |
|------|-----------|
| `LoadFilesRestControllerSpec` | Controller carga de archivos empresa |
| `FilesToSignRestControllerSpec` | Controller archivos para firmar |
| `FilesSignedRestControllerSpec` | Controller archivos firmados |
| `SignControllerSpec` | Controller firmas digitales |
| `CompanyFileServiceImplSpec` | Servicio archivos empresa |
| `FileSignedServiceImplSpec` | Servicio archivos firmados |
| `FileStorageServiceImplSpec` | Servicio almacenamiento físico |
| `FileToSignServiceImplSpec` | Servicio archivos para firmar |
| `SignServiceImplSpec` | Servicio firmas digitales |
| `ConverterSpec` | DTOs y conversiones |
| `ExceptionsSpec` | Clases de excepción |
| `GlobalExceptionHandlerSpec` | Manejador global de excepciones |
| `RolValidatorSpec` | Validador de roles |
| `JwtAuthenticationFilterSpec` | Filtro JWT |
| `JwtAccessDeniedHandlerSpec` | Handler acceso denegado |
| `JwtAuthenticationEntryPointSpec` | Entry point autenticación |
| `JwtTokenValidatorSpec` | Validador de tokens JWT |

**Reportes:**
- HTML: `build/reports/tests/test/index.html`
- JaCoCo HTML: `build/jacocoHtml/index.html`
- JaCoCo XML: `build/reports/jacoco/test/jacocoTestReport.xml`

**Umbrales de cobertura configurados:**
- Ramas: mínimo **85%**
- Líneas: mínimo **80%**

---

### 12.2 Pruebas de Arquitectura (ArchUnit)

**Framework:** ArchUnit 1.3.0 con JUnit 5  
**Comando:** `./gradlew architectureTest`

**Reglas verificadas:**

| Regla | Descripción |
|-------|-------------|
| `domain_no_debe_depende_de_nadie` | La capa `domain` no puede importar clases de `application`, `infraestrutura` ni `web` |
| `application_solo_puede_depende_de_domain` | La capa `application` solo puede depender de `domain` y librerías estándar |
| `infraestrutura_solo_puede_depende_de_application_y_domain` | `infraestrutura` no puede importar clases de `web` |
| `web_solo_puede_depende_de_application` | La capa `web` no puede importar directamente de `infraestrutura` |
| `controllers_deben_estar_en_web` | Toda clase `*Controller` debe residir en el paquete `..web.controller..` |
| `services_deben_estar_en_aplication_o_domain` | Toda interfaz `*Service` debe estar en `application.service` o `domain` |
| `no_deberia_haber_ciclos_entre_capas` | No se permiten dependencias cíclicas entre capas |
| `domain_no_debe_usar_anotaciones_de_spring` | El dominio debe estar libre de anotaciones de Spring |

---

### 12.3 Pruebas Funcionales — Karate Framework

Suite de pruebas funcionales/aceptación para **ms-loadfiles-neg**.

**Framework:** Karate 1.4.0 + JUnit 5  
**Comando principal:** `./gradlew automatedTest`  
**Tests implementados:** 343 escenarios al 100% de paso

#### Estructura de directorios

```
src/automated-test/
├── java/.../automatedtest/
│   ├── runner/
│   │   ├── SmokeTestRunner.java        # @smoke — validación rápida
│   │   ├── RegressionTestRunner.java   # @smoke + @regression — CI estándar
│   │   ├── FullTestRunner.java         # Todos los escenarios no-prod
│   │   └── ProductionSafeRunner.java   # @read-only + @production-safe
│   └── util/
│       └── JwtTestUtil.java            # Generador de JWT para tests
└── resources/
    ├── karate-config.js                # Config multiambiente (carga tokens)
    ├── karate.properties.template      # COPIAR → karate.properties (git-ignorado)
    ├── features/
    │   ├── auth/
    │   │   └── jwt-tokens.feature      # Helper callonce (no ejecutar solo)
    │   ├── loadfiles/
    │   │   ├── list-company-files.feature
    │   │   ├── get-file-by-type.feature
    │   │   ├── upload-company-file.feature
    │   │   └── get-company-file-pdf.feature
    │   ├── filessigned/
    │   │   ├── list-signed-files.feature
    │   │   └── get-signed-file-pdf.feature
    │   ├── filesign/
    │   │   ├── list-files-to-sign.feature
    │   │   ├── get-pdf-to-sign.feature
    │   │   └── upload-signed-pdf.feature
    │   └── signs/
    │       ├── save-sign.feature
    │       ├── get-sign-by-company.feature
    │       └── get-sign-by-user.feature
    ├── schemas/                        # JSON schemas para validación de respuestas
    └── testfiles/
        ├── dummy.pdf                   # PDF mínimo para pruebas multipart
        └── dummy-sign.png              # PNG mínimo para pruebas de firma
```

#### Configuración inicial (obligatoria)

**Paso 1:** Crear `karate.properties` (archivo git-ignorado):

```bash
cp src/automated-test/resources/karate.properties.template \
   src/automated-test/resources/karate.properties
```

**Paso 2:** Editar con los valores del ambiente:

```properties
karate.jwt.secret=AF84F1FGllNpNnLG055fdg5hGHJK4KGG5VH5TR5J05JFGGDFDGXVV545J4505G666JFGF2mMY95y
karate.base.url=http://localhost:8081/load
karate.test.companyId=1
karate.test.userId=1
karate.test.fileId=1
karate.test.signedFileId=1
karate.test.fileToSignId=1
karate.test.companyName=empresa-test
karate.test.ipLoad=192.168.1.100
```

> **IMPORTANTE:** `karate.properties` está en `.gitignore`. NUNCA subirlo al repositorio.

#### Ejecutar pruebas

```bash
# Suite smoke (validación rápida — ~2 min)
./gradlew automatedTest

# Solo smoke
./gradlew automatedTest -Pkarate.options="--tags @smoke"

# Regression completo
./gradlew automatedTest -Pkarate.options="--tags @regression,@smoke"

# Solo read-only (seguro para producción)
./gradlew automatedTest -Pkarate.options="--tags @read-only,@production-safe"

# Contra un ambiente específico
./gradlew automatedTest \
  -PAMBIENTE_PIPE=qa \
  -Pkarate.jwt.secret="<secret-qa>" \
  -Pkarate.base.url="http://qa-server:8081/load" \
  -Pkarate.test.companyId="5"
```

#### Tags disponibles

| Tag | Descripción | Seguro en producción |
|-----|-------------|---------------------|
| `@smoke` | Escenarios críticos mínimos (solo GET) | Sí |
| `@regression` | Suite completa de regresión | No (incluye writes) |
| `@full` | Todos los escenarios | No |
| `@read-only` | Solo escenarios sin efecto en BD | Sí |
| `@production-safe` | Idem read-only, explícitamente marcados | Sí |

> Si `AMBIENTE_PIPE=prod`, usar `ProductionSafeRunner` que ejecuta únicamente `@read-only` y `@production-safe`.

#### Matriz de cobertura funcional

| Endpoint | Auth | 401 | 403 | 200/201 | 404 | 400/422 | Schema |
|----------|------|-----|-----|---------|-----|---------|--------|
| GET /api/listofcompanyfiles/{id} | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| POST /api/companyfile/upload/file/{id} | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| POST /api/companyfile/upload | ✓ | ✓ | ✓ | ✓ | — | ✓ | ✓ |
| POST /api/companyfilepdf | ✓ | ✓ | ✓ | ✓ | — | ✓ | ✓ |
| GET /filessigned/listofcompanysignedfiles/{id} | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ |
| POST /filessigned/companyfilesignedpdf/{name} | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| GET /filesign/filestosignregistry | ✓ | ✓ | ✓ | ✓ | — | — | ✓ |
| POST /filesign/pdftosign | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| POST /filesign/uploadsignedpdf | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| POST /signs/save | ✓ | ✓ | ✓ | ✓ | — | — | ✓ |
| GET /signs/company/{id} | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — |
| GET /signs/user/{id} | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — |

#### Reportes Karate

| Reporte | Ruta |
|---------|------|
| HTML (Karate) | `build/reports/tests/automatedTest/index.html` |
| JUnit XML | `build/test-results/automatedTest/*.xml` |
| Cucumber JSON | `target/surefire-reports/` |

#### Secretos requeridos en CI para pruebas funcionales

| Secret | Descripción |
|--------|-------------|
| `KARATE_JWT_SECRET` | Secreto JWT del ambiente |
| `KARATE_BASE_URL` | URL base del servicio |
| `KARATE_TEST_COMPANY_ID` | ID de empresa en datos de prueba |
| `KARATE_TEST_USER_ID` | ID de usuario en datos de prueba |
| `KARATE_TEST_FILE_ID` | ID de archivo en datos de prueba |
| `KARATE_TEST_SIGNED_FILE_ID` | ID de archivo firmado |
| `KARATE_TEST_FILE_TO_SIGN_ID` | ID de archivo pendiente de firma |
| `KARATE_TEST_COMPANY_NAME` | Nombre de empresa para rutas de archivo |

---

### 12.4 Pruebas de Rendimiento — Apache JMeter

**Framework:** Apache JMeter 5.6+  
**Ubicación:** `external-test/jmeter/`

#### Estructura

```
external-test/jmeter/
├── config/
│   ├── environment.properties   # Documentación de ambientes disponibles
│   ├── dev.properties           # Configuración desarrollo
│   ├── qa.properties            # Configuración QA
│   └── prod.properties          # Configuración producción (solo lectura)
├── data/
│   ├── dev/   → company-ids.csv, file-ids.csv, user-ids.csv
│   ├── qa/    → ídem
│   └── prod/  → ídem (solo IDs de lectura)
└── plans/
    ├── ms-loadfiles-master.jmx  # Plan maestro (incluye los demás)
    ├── smoke/
    │   └── ms-loadfiles-smoke.jmx   # 1 hilo, 1 loop
    ├── load/
    │   └── ms-loadfiles-load.jmx    # 10 hilos / 100 hilos
    └── stress/
        └── ms-loadfiles-stress.jmx  # Estrés
```

#### Perfiles de carga (configurados en `dev.properties`)

| Perfil | Hilos | Ramp-up | Duración | Throughput |
|--------|-------|---------|---------|------------|
| Smoke | 1 | 1s | 1 loop | 100 req/min |
| Carga básica | 10 | 30s | 300s | 30 req/min |
| Carga concurrente | 100 | 120s | 600s | 100 req/min |

#### SLAs definidos

| Tipo de operación | Tiempo máximo aceptable |
|------------------|------------------------|
| Lectura (GET/POST read) | 2000 ms |
| Lectura de PDF | 5000 ms |
| Escritura (upload) | 5000 ms |
| Upload de archivo | 15000 ms |
| Guardar firma | 15000 ms |

**Umbral Apdex:**
- Satisfecho: ≤ 500 ms
- Tolerable: ≤ 1500 ms

#### Ejecutar pruebas de rendimiento

**Modo no-interactivo (CLI):**
```bash
# Smoke test en DEV
jmeter -n \
  -t external-test/jmeter/plans/ms-loadfiles-master.jmx \
  -p external-test/jmeter/config/dev.properties \
  -JAMBIENTE_PIPE=dev \
  -l external-test/jmeter/results/result-smoke.jtl \
  -e -o external-test/jmeter/reports/run-$(date +%Y%m%d-%H%M%S)

# Con ambiente específico
jmeter -n \
  -t external-test/jmeter/plans/ms-loadfiles-master.jmx \
  -p external-test/jmeter/config/qa.properties \
  -JAMBIENTE_PIPE=qa \
  -l results/result.jtl \
  -e -o reports/run-qa-$(date +%Y%m%d-%H%M%S)
```

**Con variables de entorno:**
```bash
# Windows
set AMBIENTE_PIPE=dev && jmeter -n -t plans/ms-loadfiles-master.jmx -p config/dev.properties

# Linux
AMBIENTE_PIPE=qa ./scripts/run-tests.sh
```

#### Seguridad en producción (JMeter)

Cuando `prod.safe.only=true` en `prod.properties`, los siguientes endpoints de escritura son bloqueados:

- `POST /api/companyfile/upload` — subida de archivos empresa
- `POST /filesign/uploadsignedpdf` — subida de PDF firmado
- `POST /signs/save` — guardar firma digital

Los endpoints de lectura siempre están disponibles en producción.

#### Archivos de prueba

Los planes JMeter reutilizan los mismos archivos de las pruebas Karate:

```
src/automated-test/resources/testfiles/dummy.pdf      → upload de archivos
src/automated-test/resources/testfiles/dummy-sign.png → guardar firma
```

#### Reportes de rendimiento

Los reportes HTML se generan en:
```
external-test/jmeter/reports/run-{ambiente}-{fecha}/
```

---

### 12.5 Integración CI/CD

| Sistema | Archivo | Descripción |
|---------|---------|-------------|
| GitHub Actions | `.github/workflows/functional-tests.yml` | Pruebas funcionales Karate en push/PR |
| Jenkins | `Jenkinsfile.functional` | Pipeline Jenkins para pruebas funcionales |
| GitLab CI | `.gitlab-ci-functional.yml` | Pipeline GitLab para pruebas funcionales |

---

## 13. Estructura del Proyecto

```
ms-loadfiles-neg/
├── src/
│   ├── main/
│   │   ├── java/com/loadfilesservice/loadfiles/
│   │   │   ├── LoadfilesApplication.java          # Clase principal Spring Boot
│   │   │   ├── application/
│   │   │   │   ├── ConstantVariables.java          # Constantes de rutas de archivo
│   │   │   │   ├── exception/
│   │   │   │   │   ├── ApiErrorResponse.java       # DTO de error estándar
│   │   │   │   │   ├── BadRequestException.java
│   │   │   │   │   ├── InternalServerErrorException.java
│   │   │   │   │   └── ResourceNotFoundException.java
│   │   │   │   └── service/
│   │   │   │       ├── ICompanyFileService.java
│   │   │   │       ├── IFileSignedService.java
│   │   │   │       ├── IFileStorageService.java
│   │   │   │       ├── IFileToSignService.java
│   │   │   │       └── ISignService.java
│   │   │   ├── domain/
│   │   │   │   ├── CompanyFile.java               # Entidad tabla `files`
│   │   │   │   ├── CompanyFileType.java           # Entidad tabla `file_type`
│   │   │   │   ├── FileSigned.java                # Entidad tabla `files_signed`
│   │   │   │   ├── FileToSign.java                # Entidad tabla `files_to_sign`
│   │   │   │   └── Sign.java                      # Entidad tabla `sign`
│   │   │   ├── infraestrutura/
│   │   │   │   ├── config/
│   │   │   │   │   ├── OpenApiConfig.java          # Config Swagger/OpenAPI 3
│   │   │   │   │   └── WebMvcConfig.java
│   │   │   │   ├── persistence/
│   │   │   │   │   ├── ICompanyFileDao.java        # Repositorio JPA files
│   │   │   │   │   ├── IFileSignedDao.java         # Repositorio JPA files_signed
│   │   │   │   │   ├── IFileToSignDao.java         # Repositorio JPA files_to_sign
│   │   │   │   │   └── ISignDao.java               # Repositorio JPA sign
│   │   │   │   ├── security/
│   │   │   │   │   ├── auth/
│   │   │   │   │   │   └── RolValidator.java       # Bean @rolValidator para SpEL
│   │   │   │   │   ├── config/
│   │   │   │   │   │   ├── SecurityConfig.java     # SecurityFilterChain
│   │   │   │   │   │   └── SecurityProperties.java # Bindings de security.yml
│   │   │   │   │   ├── handler/
│   │   │   │   │   │   ├── JwtAccessDeniedHandler.java
│   │   │   │   │   │   └── JwtAuthenticationEntryPoint.java
│   │   │   │   │   └── jwt/
│   │   │   │   │       ├── JwtAuthenticationFilter.java
│   │   │   │   │       └── JwtTokenValidator.java
│   │   │   │   └── usecase/
│   │   │   │       ├── CompanyFileServiceImpl.java
│   │   │   │       ├── FileSignedServiceImpl.java
│   │   │   │       ├── FileStorageServiceImpl.java
│   │   │   │       ├── FileToSignServiceImpl.java
│   │   │   │       └── SignServiceImpl.java
│   │   │   └── web/
│   │   │       ├── controller/
│   │   │       │   ├── FilesSignedRestController.java
│   │   │       │   ├── FilesToSignRestController.java
│   │   │       │   ├── LoadFilesRestController.java
│   │   │       │   └── SignController.java
│   │   │       ├── dto/
│   │   │       │   ├── CompanyFileDTORequest.java
│   │   │       │   ├── CompanyFileDTOResponse.java
│   │   │       │   ├── Converter.java
│   │   │       │   ├── FileSignedDTORequest.java
│   │   │       │   ├── FileSignedDTOResponse.java
│   │   │       │   └── FileToSignDTOResponse.java
│   │   │       └── exception/
│   │   │           └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.yml        # Config base + perfil dev activo
│   │       ├── application-dev.yml    # Credenciales hardcodeadas para DEV
│   │       ├── application-qa.yml     # Variables de entorno para QA
│   │       └── application-prod.yml   # Variables de entorno para PROD (Swagger off)
│   ├── test/
│   │   └── groovy/                    # Pruebas Spock (unitarias)
│   ├── automated-test/                # Pruebas funcionales Karate
│   │   ├── java/                      # Runners JUnit
│   │   └── resources/
│   │       ├── features/              # Escenarios .feature
│   │       └── schemas/               # JSON schemas de validación
│   └── architecture-test/
│       └── java/ArchitectureTest.java # Pruebas ArchUnit
├── external-test/
│   └── jmeter/                        # Suite de rendimiento JMeter
│       ├── config/                    # Properties por ambiente
│       ├── data/                      # CSVs de datos de prueba
│       └── plans/                     # Planes JMeter (.jmx)
├── config/
│   ├── checkstyle/checkstyle.xml
│   ├── pmd/ruleset.xml
│   └── spotbugs/exclude.xml
├── Dockerfile                         # Build multistage (JDK 17 Alpine)
├── build.gradle                       # Dependencias y plugins
├── test.gradle                        # Configuración de source sets de prueba
├── coverage.gradle                    # Config JaCoCo y umbrales
├── settings.gradle
├── sonar-project.properties
├── .github/workflows/functional-tests.yml
└── .gitlab-ci-functional.yml
```

---

## 14. Diagramas

### Arquitectura General

```mermaid
graph TB
    subgraph Cliente
        FE[Frontend Angular<br/>:4300 / :4400 / :4600]
    end

    subgraph ms-loadfiles-neg[:8081]
        direction TB
        WEB[web<br/>Controllers / DTOs / ExceptionHandler]
        APP[application<br/>Interfaces / Excepciones / Constantes]
        INFRA[infraestrutura<br/>UseCases / Security / Config]
        DOM[domain<br/>Entidades JPA]

        WEB --> APP
        INFRA --> APP
        INFRA --> DOM
        WEB --> DOM
        APP --> DOM
    end

    subgraph Actuator[:8082]
        HC[/actuator/health]
    end

    subgraph Persistencia
        MySQL[(MySQL<br/>AWS RDS<br/>dbcompany)]
        FS[Sistema de Archivos<br/>uploadfiles/<br/>files_registry_signed/<br/>files_to_sign/<br/>signatures/]
    end

    subgraph Seguridad
        SEC[ms-security<br/>Emisor JWT]
    end

    FE -->|HTTP + JWT Bearer| WEB
    INFRA -->|JPA/JDBC| MySQL
    INFRA -->|File I/O| FS
    FE -->|Obtiene JWT| SEC
```

---

### Flujo Principal de Negocio

```mermaid
sequenceDiagram
    actor Empresa
    actor Operario
    participant API as ms-loadfiles-neg
    participant BD as MySQL
    participant FS as Sistema de Archivos

    Empresa->>API: POST /api/companyfile/upload (PDF + JWT)
    API->>BD: findByCompanyAndCompanyFileType(companyId, tipo)
    BD-->>API: archivo activo previo (si existe)
    API->>BD: save(archivoActivo, state=0) [desactivar]
    API->>FS: deleteFile(archivoActivo)
    API->>FS: copyFile(nuevoPDF) → UUID_nombre.pdf
    API->>BD: save(nuevoCompanyFile, state=1)
    API-->>Empresa: 201 Created {savedFile}

    Operario->>API: GET /filesign/filestosignregistry (JWT)
    API->>BD: findByCompanyFileTypeAndState(10, 1)
    BD-->>API: [FileToSign list]
    API-->>Operario: 200 OK [lista archivos]

    Operario->>API: POST /filesign/uploadsignedpdf (PDF firmado + JWT)
    API->>FS: createFolder(files_registry_signed/EMPRESA)
    API->>FS: copyFile(pdfFirmado)
    API->>BD: save(FileSigned, state=1)
    API-->>Operario: 201 Created {saveFile: true}
```

---

### Flujo de una Solicitud HTTP

```mermaid
sequenceDiagram
    participant C as Cliente
    participant F as JwtAuthenticationFilter
    participant SC as SecurityContext
    participant CT as Controller
    participant RV as RolValidator
    participant SV as Service (UseCase)
    participant DAO as Repository (DAO)
    participant BD as MySQL

    C->>F: HTTP Request + Authorization: Bearer <token>
    F->>F: extractToken(header)
    F->>F: validateToken(token) — firma, expiración
    F->>F: extractUsername + extractRol
    F->>SC: setAuthentication(username, rol)
    F->>CT: continúa cadena de filtros
    CT->>RV: @PreAuthorize("@rolValidator.hasRol(auth, 'carga')")
    RV->>RV: compara rol del token con lista del grupo
    alt rol permitido
        RV-->>CT: true
        CT->>SV: delegación de lógica
        SV->>DAO: consulta/persistencia
        DAO->>BD: SQL
        BD-->>DAO: resultado
        DAO-->>SV: entidad
        SV-->>CT: resultado de negocio
        CT-->>C: ResponseEntity (200/201/404)
    else rol no permitido
        RV-->>CT: false (AccessDeniedException)
        CT-->>C: 403 Forbidden + ApiErrorResponse
    end
```

---

### Dependencias entre Capas

```mermaid
graph LR
    subgraph web
        C[Controllers]
        D[DTOs]
        EH[GlobalExceptionHandler]
    end

    subgraph application
        SI[Service Interfaces]
        EX[Exceptions]
        CV[ConstantVariables]
    end

    subgraph infraestrutura
        UC[UseCases / Impl]
        DAO[DAOs / JPA]
        SEC[Security]
        CFG[Config]
    end

    subgraph domain
        ENT[Entities JPA]
    end

    C --> SI
    C --> ENT
    UC --> SI
    UC --> DAO
    UC --> ENT
    UC --> EX
    DAO --> ENT
    SEC --> CFG

    style domain fill:#d4f1c4
    style application fill:#c4ddf1
    style infraestrutura fill:#f1e4c4
    style web fill:#f1c4c4
```

---

## 15. Ambientes

### Desarrollo (DEV)

```yaml
URL base:    http://localhost:8081
Actuator:    http://localhost:8082/actuator/health
BD:          MySQL AWS RDS (credenciales hardcodeadas en application-dev.yml)
ddl-auto:    update
Swagger:     Habilitado
Log SQL:     DEBUG
Log app:     DEBUG
Log security: DEBUG
```

### QA

```yaml
URL base:    http://<qa-host>:8081/load
Actuator:    http://<qa-host>:8082/actuator/health
BD:          MySQL (variables de entorno: DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD)
ddl-auto:    validate
Swagger:     Habilitado
Log SQL:     WARN
Log app:     INFO
Log security: INFO
Pool BD:     10 conexiones máx
```

### Producción (PROD)

```yaml
URL base:    http://<prod-host>:8081/load
Actuator:    http://<prod-host>:8082/actuator/health
BD:          MySQL con SSL obligatorio (requireSSL=true)
ddl-auto:    none (el esquema se gestiona fuera del microservicio)
Swagger:     DESHABILITADO (api-docs.enabled: false)
Log SQL:     ERROR
Log app:     WARN
Log security: WARN
Pool BD:     20 conexiones máx, 5 idle mínimo
```

---

## 16. Troubleshooting

### Puerto 8081 ocupado

```
Error: Address already in use: 8081
```

**Solución:**
```bash
# Linux/Mac
lsof -i :8081 && kill -9 <PID>

# Windows
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### Error de conexión a la base de datos

```
HikariPool: Connection refused / Communications link failure
```

**Causas y soluciones:**
- Verificar que el host MySQL está accesible: `ping dbcompany.cn48sgciaax1.us-east-2.rds.amazonaws.com`
- Verificar credenciales en `application-dev.yml`
- Verificar que la base de datos `dbcompany` existe y las tablas están creadas
- En DEV, verificar conectividad de red con AWS RDS (VPN si aplica)

### Error JWT: `Firma JWT inválida`

```
SignatureException: Firma JWT inválida
```

**Causas y soluciones:**
- El token fue generado con un secret diferente al configurado en `security.jwt.secret`
- Verificar que el secret en `karate.properties` coincide con el de `application.yml`
- Si se cambió el secret en QA/PROD, todos los tokens previos son inválidos

### Error JWT: `Token JWT expirado`

```
ExpiredJwtException: Token JWT expirado
```

**Solución:** Obtener un nuevo token desde `ms-security` y reintentar.

### Swagger no disponible (404)

**Causas:**
- Perfil `prod` activo: Swagger está deshabilitado en producción
- Verificar `spring.profiles.active` configurado

**Verificar perfil activo:**
```bash
curl http://localhost:8081/actuator/env | grep profiles
```

### Archivo no encontrado en disco (`InternalServerErrorException`)

```
Error interno: El archivo no se encuentra o no es legible: uuid_nombre.pdf
```

**Causas y soluciones:**
- El directorio de archivos no existe o cambió de ruta
- El archivo fue eliminado manualmente del sistema de archivos
- En contenedor Docker: el volumen de archivos no está montado correctamente
- Verificar que las carpetas `uploadfiles/`, `files_registry_signed/`, `signatures/` existen en el directorio de trabajo de la aplicación

### Error de configuración faltante (variables de entorno)

```
Could not resolve placeholder '${DB_HOST}'
```

**Causa:** El perfil `qa` o `prod` está activo pero las variables de entorno no están definidas.

**Solución:**
```bash
export DB_HOST=<host>
export DB_NAME=dbcompany
export DB_USERNAME=<usuario>
export DB_PASSWORD=<contraseña>
export JWT_SECRET=<secreto-base64>
export SPRING_PROFILES_ACTIVE=qa
```

### Checkstyle / PMD fallan en el build

```
Checkstyle rule violations were found
```

**Solución:**
```bash
# Ver reporte detallado
./gradlew checkstyleMain
# Reporte en: build/reports/checkstyle/main.html
```

### Cobertura insuficiente (JaCoCo)

```
Rule violated for bundle ms-loadfiles-neg: branches covered ratio is 0.83, but expected minimum is 0.85
```

**Solución:** Agregar casos de prueba para las ramas no cubiertas. Ver reporte en `build/jacocoHtml/index.html`.

---

## 17. Consideraciones Técnicas

### Decisiones arquitectónicas identificadas

1. **Arquitectura hexagonal verificada por ArchUnit:** Las reglas de dependencia entre capas se verifican automáticamente en cada build mediante pruebas de arquitectura. Esto garantiza que la arquitectura no se degrada con el tiempo.

2. **Almacenamiento de archivos en sistema de archivos local:** Los archivos se guardan en el sistema de archivos del servidor (no en base de datos ni en S3). En producción, estas carpetas deben estar en un volumen persistente compartido si hay más de una instancia.

3. **Estrategia de reemplazo activo/inactivo:** En lugar de eliminar registros de base de datos, se usa `state = 0` para inactivar y `state = 1` para el activo. Esto mantiene el historial de versiones pero puede crecer ilimitadamente.

4. **JWT solo de validación:** Este microservicio no emite tokens, solo los valida. Depende del secreto compartido con `ms-security`. Un cambio de secreto requiere sincronización.

5. **Roles configurados en YAML:** Los grupos de roles no están hardcodeados en el código sino en `application.yml`. Esto permite ajustar permisos sin recompilar, pero requiere reinicio.

6. **Actuator en puerto separado:** El puerto `8082` para management garantiza que los health checks no compiten con el tráfico de negocio y pueden configurarse con reglas de firewall diferentes.

7. **Migración de Maven a Gradle:** El proyecto fue migrado de Maven a Gradle (Gradle 8.14) para aprovechar builds incrementales y configuración más flexible de source sets (pruebas por tipo).

### Patrones utilizados

- **Ports and Adapters (Hexagonal):** Interfaces de servicio en `application`, implementaciones en `infraestrutura`
- **DTO Pattern:** Objetos de transferencia (`DTORequest`, `DTOResponse`) separados de entidades de dominio
- **Strategy Pattern (roles):** Los grupos de roles se resuelven dinámicamente desde la configuración
- **Template Method (GlobalExceptionHandler):** Manejo centralizado de excepciones con `@RestControllerAdvice`
- **Repository Pattern:** Interfaces `IxxxDao` extendiendo `CrudRepository` / `JpaRepository`
- **Active Record State:** Manejo de archivos activos/inactivos con campo `state`

### Limitaciones conocidas

- **Sin S3 o almacenamiento distribuido:** Los archivos físicos requieren un volumen compartido en despliegues con múltiples instancias.
- **Sin paginación:** Los endpoints de listado retornan todos los registros sin paginación. Puede ser un problema con volúmenes grandes de datos.
- **CORS hardcodeado:** Los orígenes CORS están configurados como constantes en los controladores, no en configuración externa.
- **`spring.application.name`:** Está configurado como `ms-security` en `application.yml` en lugar de `ms-loadfiles-neg`. Pendiente de corrección.
- **Sin tracing distribuido:** No hay integración con sistemas de trazabilidad distribuida (Zipkin, Jaeger, OpenTelemetry).
- **Directorio de trabajo:** Las rutas de archivo son relativas al directorio de trabajo del proceso Java. Esto puede ser inconsistente en diferentes ambientes si no se controla el `WORKDIR`.

### Recomendaciones técnicas

- Implementar paginación en los endpoints de listado (`/api/listofcompanyfiles`, `/filessigned/listofcompanysignedfiles`).
- Migrar el almacenamiento de archivos a un servicio de objetos (AWS S3, MinIO) para escalabilidad horizontal.
- Corregir `spring.application.name` de `ms-security` a `ms-loadfiles-neg` en `application.yml`.
- Configurar CORS mediante `WebMvcConfigurer` centralizado en lugar de anotaciones por controlador.
- Agregar tracing distribuido (Micrometer Tracing + Zipkin/Jaeger) para observabilidad end-to-end.
- Considerar un mecanismo de limpieza periódica de registros inactivos (`state = 0`) para controlar el crecimiento de la base de datos.
- Evaluar agregar el endpoint `DELETE` para desactivar archivos explícitamente desde la UI, en lugar de solo al reemplazar.

---

## 18. Changelog

### v1.0.0 — 2026-06-23 (rama: feature-readme)

| PR | Rama | Descripción |
|----|------|-------------|
| #17 | `feature-.gitignore` | Actualizar `.gitignore`: Gradle, logs, `.env`, temp y `files_to_sign` |
| #16 | `feature-sawwger` | Implementar Swagger/OpenAPI 3 con springdoc-openapi 2.6.0 |
| #15 | `feature-collections` | Agregar colección Postman de smoke tests para todos los endpoints |
| #14 | `feature-docker` | Configurar Dockerfile multistage (JDK 17 Alpine) + JPQL JOIN FETCH |
| #13 | `feature-healhtcheck` | Configurar Spring Boot Actuator con health checks y liveness/readiness probes |
| #12 | `feature-pruebas-funcionales` | Implementar suite de pruebas de rendimiento JMeter con 3 perfiles y cobertura multi-ambiente |
| #11 | `feature-pruebas-funcionales` | Implementar suite completa de pruebas funcionales Karate con 343 tests al 100% |
| #10 | `fix-feature-checkstyle` | Corregir 3 errores SpotBugs `RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT` |
| #9 | `fix-feature-checkstyle` | Implementar suite completa de pruebas Spock con cobertura ~99% líneas / ~97% ramas |
| #8 | `feature-pruebas-arquitectura` | Implementar pruebas de arquitectura hexagonal con ArchUnit |
| #7 | `feature-checkstyle` | Configurar Checkstyle, PMD, SpotBugs y SonarQube |
| #6 | `feature-sonar` | Corregir issues de calidad SonarQube (2 iteraciones) |
| #5 | `feature-solid` | Refactorizar clases con violaciones SOLID (SRP, ISP, DIP) |
| #4 | `feature-securizacion` | Implementar seguridad JWT con Spring Security 6 y autorización por roles |
| #3 | `feature-codigos-endpoints` | Implementar manejo centralizado de excepciones y códigos HTTP REST correctos |
| #2, #1 | `feature-migratcion-maven-gradle` | Migrar de Maven a Gradle 8.14 y convertir configuración de `.properties` a `.yml` |
| — | — | Commits iniciales: creación de entidades, carga de archivos, firmas digitales, validación de usuarios |

---
