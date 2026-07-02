package com.loadfilesservice.loadfiles.web.controller;

import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadfilesservice.loadfiles.application.ConstantVariables;
import com.loadfilesservice.loadfiles.application.exception.ApiErrorResponse;
import com.loadfilesservice.loadfiles.application.exception.BadRequestException;
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import com.loadfilesservice.loadfiles.web.dto.CompanyFileDTORequest;
import com.loadfilesservice.loadfiles.web.dto.CompanyFileDTOResponse;
import com.loadfilesservice.loadfiles.web.dto.Converter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Controlador REST para la carga y consulta de archivos de empresa. */
@Tag(name = "Archivos de Empresa", description = "Operaciones de carga y consulta de archivos de empresa")
@CrossOrigin(origins = {"http://localhost:4300", "http://localhost:4600"})
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class LoadFilesRestController {

    private final ICompanyFileService companyFileService;

    private final IFileStorageService fileStorageService;

    private final Converter converter;

    private final ObjectMapper objectMapper;

    /** Sube un archivo de empresa y registra el cambio en la base de datos. */
    @Operation(summary = "Subir archivo de empresa",
        description = "Sube un archivo de empresa en formato multipart/form-data y lo registra en BD. "
            + "Roles requeridos: ADMINISTRADOR, EMPRESA, OPERARIO.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Archivo subido y registrado exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Formato JSON del campo fileinfo inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(value = "/companyfile/upload", produces = "application/json")
    public ResponseEntity<?> uploadFile(
            @Parameter(description = "Archivo PDF a subir") @RequestParam("file") MultipartFile newfile,
            @Parameter(description = "Información del archivo en JSON. Campos: ipLoad (string), company (long), companyFileType")
            @RequestParam("fileinfo") String jsonCompanyFile) {

        Map<String, Object> response = new HashMap<>();
        CompanyFileDTORequest companyFileDTO;
        try {
            companyFileDTO = this.objectMapper.readValue(jsonCompanyFile, CompanyFileDTORequest.class);
        } catch (JsonProcessingException e) {
            log.error("[LoadFilesRestController][uploadFile][loadfiles] Error al parsear el JSON del archivo", e);
            throw new BadRequestException("El formato del JSON de información del archivo es inválido");
        }

        if (!newfile.isEmpty()) {
            CompanyFile companyFileBase = this.converter.companyFileDTOtoCompanyFile(companyFileDTO);
            CompanyFile savedFile = this.companyFileService.replaceCompanyFile(newfile, companyFileBase);

            log.info("[LoadFilesRestController][uploadFile][loadfiles] Archivo guardado: {}", savedFile);

            response.put("savedFile", savedFile);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Retorna el nombre del archivo activo de una empresa por tipo. */
    @Operation(summary = "Obtener nombre de archivo activo por empresa y tipo",
        description = "Retorna el nombre del archivo activo de una empresa filtrado por tipo de archivo. "
            + "Roles requeridos: todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nombre del archivo retornado exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno al obtener el archivo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/companyfile/upload/file/{id}")
    public ResponseEntity<?> getFile(
            @Valid @RequestBody CompanyFileType fileType,
            @Parameter(description = "ID de la empresa", example = "1") @PathVariable Long id) {

        Map<String, Object> response = new HashMap<>();

        List<CompanyFile> listCompanyFile;
        try {
            listCompanyFile = this.companyFileService.findByCompanyAndCompanyFileType(id, fileType);
        } catch (Exception e) {
            log.error("[LoadFilesRestController][getFile][loadfiles] Error al intentar obtener el archivo", e);
            throw new InternalServerErrorException("Error al intentar obtener el archivo", e);
        }

        listCompanyFile.stream()
                .filter(cf -> Long.valueOf(1L).equals(cf.getState()))
                .findFirst()
                .ifPresent(cf -> response.put("fileName", cf.getOriginalFileName()));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** Retorna el listado de archivos activos de una empresa. */
    @Operation(summary = "Listar archivos activos de empresa",
        description = "Retorna todos los archivos activos (estado=1) asociados a una empresa. "
            + "Roles requeridos: todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de archivos retornada exitosamente",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = CompanyFileDTOResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No se encontraron archivos para la empresa",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @GetMapping(value = "/listofcompanyfiles/{id}", produces = "application/json")
    public ResponseEntity<?> getListOfCompanyFiles(
            @Parameter(description = "ID de la empresa", example = "1") @PathVariable Long id) {
        List<CompanyFile> companyFiles = this.companyFileService.findByCompanyAndState(id, 1L);

        if (companyFiles.isEmpty()) {
            log.error("[LoadFilesRestController][getListOfCompanyFiles][loadfiles] No hay archivos de la empresa en la BD");
            throw new ResourceNotFoundException("No hay archivos de la empresa en la BD");
        } else {
            List<CompanyFileDTOResponse> companyFilesDTO = companyFiles.stream()
                    .map(this.converter::companyFileToDTO)
                    .toList();

            return ResponseEntity.status(HttpStatus.OK).body(companyFilesDTO);
        }
    }

    /** Retorna el PDF de un archivo de empresa como arreglo de bytes. */
    @Operation(summary = "Obtener PDF de archivo de empresa",
        description = "Retorna el contenido binario del PDF de un archivo de empresa. "
            + "Roles requeridos: todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF retornado exitosamente",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error al obtener o leer el archivo PDF",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @PostMapping("/companyfilepdf")
    public ResponseEntity<byte[]> getPdfCompanyFile(@Valid @RequestBody CompanyFile file) {
        Optional<CompanyFile> companyFileFoundedOpt;
        try {
            companyFileFoundedOpt = this.companyFileService.findById(file.getId());
        } catch (Exception e) {
            log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg] Error al intentar obtener el archivo", e);
            throw new InternalServerErrorException("Error al intentar obtener el archivo", e);
        }

        CompanyFile companyFileFounded = companyFileFoundedOpt
                .orElseThrow(() -> new InternalServerErrorException("Error al intentar obtener el archivo"));

        Resource resource;
        try {
            resource = this.fileStorageService.loadFile(companyFileFounded.getFileName(), ConstantVariables.PATH_UPLOADS);
        } catch (MalformedURLException e) {
            log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg] Error al intentar obtener el archivo: {}",
                    companyFileFounded.getFileName());
            throw new InternalServerErrorException("Error al intentar obtener el archivo: " + companyFileFounded.getFileName());
        }

        File fileGetted;
        try {
            fileGetted = resource.getFile();
        } catch (IOException e) {
            log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg]"
                    + " Error al intentar obtener el archivo del recurso: {}",
                    companyFileFounded.getFileName(), e);
            throw new InternalServerErrorException(
                    "Error al intentar obtener el archivo del recurso: "
                            + companyFileFounded.getFileName(), e);
        }

        byte[] pdfBytes;
        try {
            pdfBytes = Files.readAllBytes(fileGetted.toPath());
        } catch (IOException e) {
            log.error("[LoadFilesRestController][getPdfCompanyFile][ms-loadfiles-neg] Error al intentar los bytes del archivo: {}",
                    companyFileFounded.getFileName(), e);
            throw new InternalServerErrorException("Error al intentar los bytes del archivo: " + companyFileFounded.getFileName(), e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline").filename(companyFileFounded.getFileName()).build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

}
