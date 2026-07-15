package com.loadfilesservice.loadfiles.web.controller;

import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.application.ConstantVariables;
import com.loadfilesservice.loadfiles.application.exception.ApiErrorResponse;
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.IFileSignedService;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.domain.FileSigned;
import com.loadfilesservice.loadfiles.web.dto.Converter;
import com.loadfilesservice.loadfiles.web.dto.FileSignedDTOResponse;
import com.loadfilesservice.loadfiles.web.dto.FileSignedRejectRequestDTO;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador REST para operaciones sobre archivos firmados. */
@Tag(name = "Archivos Firmados", description = "Operaciones de consulta, revisión y descarga de archivos firmados")
@CrossOrigin(origins = {"http://localhost:4300", "http://localhost:4500", "http://localhost:4600"})
@RestController
@RequestMapping("/filessigned")
@RequiredArgsConstructor
@Slf4j
public class FilesSignedRestController {

    private final IFileSignedService fileSignedService;

    private final IFileStorageService fileStorageService;

    private final Converter converter;

    /** Retorna la lista de archivos firmados activos de una empresa. */
    @Operation(summary = "Listar archivos firmados activos de empresa",
        description = "Retorna todos los archivos firmados activos (estado=1) de una empresa. "
            + "Roles requeridos: todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de archivos firmados retornada exitosamente",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = FileSignedDTOResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No se encontraron archivos firmados para la empresa",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @GetMapping(value = "/listofcompanysignedfiles/{id}", produces = "application/json")
    public ResponseEntity<List<FileSignedDTOResponse>> getListOfCompanySignedFiles(
            @Parameter(description = "ID de la empresa", example = "1") @PathVariable Long id) {
        List<FileSigned> filesSigned = this.fileSignedService.findByCompanyAndState(id, 1L);

        if (filesSigned.isEmpty()) {
            log.error("[FilesSignedRestController][getListOfCompanySignedFiles][loadfiles]"
                    + " No hay archivos firmados de la empresa en la BD");
            throw new ResourceNotFoundException("No hay archivos firmados de la empresa en la BD");
        } else {
            List<FileSignedDTOResponse> filesSignedDTO = filesSigned.stream()
                    .map(this.converter::fileSignedToDTO)
                    .toList();
            return ResponseEntity.status(HttpStatus.OK).body(filesSignedDTO);
        }
    }

    /** Aprueba un documento firmado. */
    @Operation(summary = "Aprobar un documento firmado",
        description = "Marca el documento firmado como aprobado. Roles requeridos: ADMINISTRADOR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Documento firmado aprobado exitosamente"),
        @ApiResponse(responseCode = "404", description = "El documento no existe"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("@rolValidator.hasRol(authentication, 'administracion')")
    @PatchMapping(value = "/{id}/approve", produces = "application/json")
    public ResponseEntity<FileSignedDTOResponse> approveFile(
            @Parameter(description = "ID del archivo firmado", example = "1") @PathVariable Long id) {
        FileSigned approved = this.fileSignedService.approve(id);
        return ResponseEntity.ok(this.converter.fileSignedToDTO(approved));
    }

    /** Rechaza un documento firmado y le notifica el motivo por correo. */
    @Operation(summary = "Rechazar un documento firmado",
        description = "Marca el documento firmado como rechazado y notifica por correo a la empresa con el motivo "
            + "indicado, incluyendo un enlace de un solo uso para volver a cargarlo. Roles requeridos: ADMINISTRADOR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Documento firmado rechazado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Falta el motivo del rechazo o el correo de la empresa"),
        @ApiResponse(responseCode = "404", description = "El documento no existe"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PreAuthorize("@rolValidator.hasRol(authentication, 'administracion')")
    @PatchMapping(value = "/{id}/reject", consumes = "application/json", produces = "application/json")
    public ResponseEntity<FileSignedDTOResponse> rejectFile(
            @Parameter(description = "ID del archivo firmado", example = "1") @PathVariable Long id,
            @Valid @RequestBody FileSignedRejectRequestDTO request) {
        FileSigned rejected = this.fileSignedService.reject(
                id, request.getRejectionReason(), request.getCompanyEmail(), request.getCompanyName());
        return ResponseEntity.ok(this.converter.fileSignedToDTO(rejected));
    }

    /** Retorna el PDF firmado de una empresa como arreglo de bytes. */
    @Operation(summary = "Obtener PDF firmado de empresa",
        description = "Retorna el contenido binario del PDF firmado de una empresa. "
            + "Roles requeridos: todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF firmado retornado exitosamente",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error al obtener o leer el archivo PDF firmado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @PostMapping("/companyfilesignedpdf/{companyName}")
    public ResponseEntity<byte[]> getPdfCompanyFileSigned(
            @Valid @RequestBody FileSigned file,
            @Parameter(description = "Nombre identificador de la empresa", example = "EMPRESA_SA")
            @PathVariable String companyName) {
        Optional<FileSigned> companyFileSignedFoundedOpt;
        try {
            companyFileSignedFoundedOpt = this.fileSignedService.findById(file.getId());
        } catch (Exception e) {
            log.error("[FilesSignedRestController][getPdfCompanyFileSigned][ms-loadfiles-neg] Error al intentar obtener el archivo", e);
            throw new InternalServerErrorException("Error al intentar obtener el archivo", e);
        }

        FileSigned companyFileSignedFounded = companyFileSignedFoundedOpt
                .orElseThrow(() -> new InternalServerErrorException("Error al intentar obtener el archivo"));

        Resource resource;
        try {
            resource = this.fileStorageService.loadFile(companyFileSignedFounded.getFileName(),
                    ConstantVariables.FILES_REGISTRY_SIGNED + "/" + companyName);
        } catch (MalformedURLException e) {
            log.error("[FilesSignedRestController][getPdfCompanyFileSigned][ms-loadfiles-neg] Error al intentar obtener el archivo: {}",
                    companyFileSignedFounded.getFileName());
            throw new InternalServerErrorException("Error al intentar obtener el archivo: " + companyFileSignedFounded.getFileName());
        }

        File fileGetted;
        try {
            fileGetted = resource.getFile();
        } catch (IOException e) {
            log.error("[FilesSignedRestController][getPdfCompanyFileSigned][ms-loadfiles-neg] "
                    + "Error al intentar obtener el archivo del recurso: {}",
                    companyFileSignedFounded.getFileName(), e);
            throw new InternalServerErrorException(
                    "Error al intentar obtener el archivo del recurso: "
                            + companyFileSignedFounded.getFileName(), e);
        }

        byte[] pdfBytes;
        try {
            pdfBytes = Files.readAllBytes(fileGetted.toPath());
        } catch (IOException e) {
            log.error("[FilesSignedRestController][getPdfCompanyFileSigned][ms-loadfiles-neg] "
                    + "Error al intentar pasar a bytes del archivo: {}",
                    companyFileSignedFounded.getFileName(), e);
            throw new InternalServerErrorException(
                    "Error al intentar pasar a bytes del archivo: "
                            + companyFileSignedFounded.getFileName(), e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline").filename(companyFileSignedFounded.getFileName()).build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

}
