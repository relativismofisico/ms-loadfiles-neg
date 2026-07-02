package com.loadfilesservice.loadfiles.web.controller;

import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.time.LocalDate;
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
import com.loadfilesservice.loadfiles.application.service.IFileSignedService;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.application.service.IFileToSignService;
import com.loadfilesservice.loadfiles.application.service.IPdfFormFillerService;
import com.loadfilesservice.loadfiles.domain.FileSigned;
import com.loadfilesservice.loadfiles.domain.FileToSign;
import com.loadfilesservice.loadfiles.web.dto.Converter;
import com.loadfilesservice.loadfiles.web.dto.FileSignedDTORequest;
import com.loadfilesservice.loadfiles.web.dto.FileToSignDTOResponse;
import com.loadfilesservice.loadfiles.web.dto.PdfToSignRequestDTO;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Controlador REST para operaciones sobre archivos pendientes de firma. */
@Tag(name = "Archivos para Firmar", description = "Operaciones sobre archivos pendientes de firma y carga de firmados")
@CrossOrigin(origins = {"http://localhost:4300", "http://localhost:4400"})
@RestController
@RequestMapping("/filesign")
@RequiredArgsConstructor
@Slf4j
public class FilesToSignRestController {

    private final IFileToSignService fileToSignService;

    private final IFileSignedService fileSignedService;

    private final IFileStorageService fileStorageService;

    private final IPdfFormFillerService pdfFormFillerService;

    private final Converter converter;

    private final ObjectMapper objectMapper;

    /** Retorna el listado de archivos de registro de empresa pendientes de firma. */
    @Operation(summary = "Listar archivos de registro pendientes de firma",
        description = "Retorna los archivos de tipo registro de empresa (tipo 10) pendientes de firma (estado=1). "
            + "Roles requeridos: ADMINISTRADOR, OPERARIO, FONDEADOR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de archivos pendientes de firma retornada exitosamente",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = FileToSignDTOResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No se encontraron archivos pendientes de firma",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping(value = "/filestosignregistry", produces = "application/json")
    public ResponseEntity<?> getFileToSignCompanyRegistry() {

        List<FileToSign> filesToSign = this.fileToSignService.findByCompanyFileTypeAndState(10L, 1L);

        if (filesToSign.isEmpty()) {
            log.error("[FilesToSignRestController][getFileToSignCompanyRegistry][ms-loadfiles-neg] "
                    + "No hay archivos de registro de empresa para firmar en la BD");
            throw new ResourceNotFoundException("No hay archivos de registro de empresa para firmar en la BD");
        } else {
            List<FileToSignDTOResponse> filesToSignDTO = filesToSign.stream()
                    .map(this.converter::fileToSignToDTO)
                    .toList();

            return ResponseEntity.status(HttpStatus.OK).body(filesToSignDTO);
        }
    }

    /** Retorna el PDF de un archivo pendiente de firma como arreglo de bytes. */
    @Operation(summary = "Obtener PDF pendiente de firma",
        description = "Retorna el contenido binario del PDF de un archivo pendiente de firma. "
            + "Roles requeridos: ADMINISTRADOR, OPERARIO, FONDEADOR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF pendiente de firma retornado exitosamente",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error al obtener o leer el archivo PDF",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/pdftosign")
    public ResponseEntity<byte[]> getPdfToSign(@Valid @RequestBody PdfToSignRequestDTO request) {
        Optional<FileToSign> companyFileFounded;
        try {
            companyFileFounded = this.fileToSignService.findById(request.getId());
        } catch (Exception e) {
            log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg] Error al intentar obtener el archivo", e);
            throw new InternalServerErrorException("Error al intentar obtener el archivo", e);
        }

        FileToSign fileToSign = companyFileFounded
                .orElseThrow(() -> new InternalServerErrorException("Error al intentar obtener el archivo"));

        Resource resource;
        try {
            resource = this.fileStorageService.loadFile(fileToSign.getFileName(), ConstantVariables.FILES_REGISTRY_SIGN);
        } catch (MalformedURLException e) {
            log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg] Error al intentar obtener el archivo: {}",
                    fileToSign.getFileName());
            throw new InternalServerErrorException("Error al intentar obtener el archivo: " + fileToSign.getFileName());
        }

        File fileGetted;
        try {
            fileGetted = resource.getFile();
        } catch (IOException e) {
            log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg] Error al intentar obtener el archivo del recurso: {}",
                    fileToSign.getFileName(), e);
            throw new InternalServerErrorException("Error al intentar obtener el archivo del recurso: " + fileToSign.getFileName(), e);
        }

        byte[] templateBytes;
        try {
            templateBytes = Files.readAllBytes(fileGetted.toPath());
        } catch (IOException e) {
            log.error("[FilesToSignRestController][getPdfToSign][ms-loadfiles-neg] Error al intentar los bytes del archivo: {}",
                    fileToSign.getFileName(), e);
            throw new InternalServerErrorException("Error al intentar los bytes del archivo: " + fileToSign.getFileName(), e);
        }

        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put("nombreEmpresa", request.getCompanyName());
        fieldValues.put("nit", request.getNit());
        fieldValues.put("nombreRepresentante", request.getRepresentativeName());
        fieldValues.put("fecha", LocalDate.now().toString());

        byte[] pdfBytes = this.pdfFormFillerService.fill(templateBytes, fieldValues);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline").filename(fileToSign.getFileName()).build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /** Sube un PDF firmado y lo registra en la base de datos. */
    @Operation(summary = "Subir PDF firmado",
        description = "Sube un archivo PDF firmado en formato multipart/form-data y lo registra en BD. "
            + "Roles requeridos: ADMINISTRADOR, OPERARIO.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "PDF firmado subido y registrado exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Archivo vacío o formato JSON de fileinfo inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/uploadsignedpdf")
    public ResponseEntity<?> uploadSignedPdf(
            @Parameter(description = "Archivo PDF firmado a subir") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Información del archivo firmado en JSON. Campos: originalFileName, ipLoad, companyName, company, user, companyFileType")
            @RequestParam("fileinfo") String jsoSignedFile) {

        Map<String, Object> response = new HashMap<>();
        FileSignedDTORequest fileSignedDTO;
        try {
            fileSignedDTO = this.objectMapper.readValue(jsoSignedFile, FileSignedDTORequest.class);
        } catch (JsonProcessingException e) {
            log.error("[FilesToSignRestController][uploadSignedPdf][loadfiles] Error al parsear el JSON del archivo firmado", e);
            throw new BadRequestException("El formato del JSON de información del archivo firmado es inválido");
        }

        if (file.isEmpty()) {
            log.error("[FilesToSignRestController][uploadSignedPdf][loadfiles] No hay un archivo pdf firmado para subir");
            throw new BadRequestException("No hay un archivo pdf firmado para subir");
        }

        FileSigned fileSignedBase = this.converter.fileSignedDtoToFileSigned(fileSignedDTO);
        this.fileSignedService.saveSignedFile(file, fileSignedBase, fileSignedDTO.getCompanyName());

        response.put("saveFile", true);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
