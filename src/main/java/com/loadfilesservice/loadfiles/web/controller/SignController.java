package com.loadfilesservice.loadfiles.web.controller;

import java.net.MalformedURLException;
import java.util.Optional;

import com.loadfilesservice.loadfiles.application.exception.ApiErrorResponse;
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.application.service.ISignService;
import com.loadfilesservice.loadfiles.domain.Sign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Controlador REST para operaciones sobre firmas digitales. */
@Tag(name = "Firmas Digitales", description = "Operaciones de registro y consulta de firmas digitales")
@CrossOrigin(origins = {"http://localhost:4300", "http://localhost:4400"})
@RestController
@RequestMapping("/signs")
@RequiredArgsConstructor
@Slf4j
public class SignController {

    private final ISignService signService;

    private final IFileStorageService fileStorageService;

    /** Guarda o reemplaza la firma activa de una empresa. */
    @Operation(summary = "Guardar o reemplazar firma de empresa",
        description = "Sube una imagen de firma y la asocia a una empresa o usuario, reemplazando la firma activa previa. "
            + "Roles requeridos: ADMINISTRADOR, EMPRESA.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Firma guardada exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Sign.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/save")
    public ResponseEntity<?> saveSign(
            @Parameter(description = "Imagen de la firma (PNG, JPG)") @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID de la empresa", example = "1") @RequestParam("company") Long company,
            @Parameter(description = "ID del usuario (0 si es firma de empresa)", example = "0") @RequestParam("user") Long user,
            @Parameter(description = "Dirección IP desde la que se carga la firma") @RequestParam("ipLoad") String ipLoad,
            @Parameter(description = "Nombre identificador de la empresa", example = "EMPRESA_SA") @RequestParam("companyName") String companyName) {

        Long effectiveUser;
        if (Long.valueOf(0L).equals(user)) {
            effectiveUser = null;
        } else {
            effectiveUser = user;
        }
        Sign savedSign = this.signService.replaceSign(file, company, effectiveUser, ipLoad, companyName);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSign);
    }

    /** Retorna el archivo de firma activa de una empresa. */
    @Operation(summary = "Obtener firma activa de empresa",
        description = "Retorna el archivo de imagen de la firma activa asociada a una empresa. "
            + "Roles requeridos: todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Firma retornada exitosamente",
            content = @Content(mediaType = "application/octet-stream",
                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No se encontró firma activa para la empresa",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error al cargar el archivo de firma",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/company/{companyId}")
    public ResponseEntity<Resource> getActiveSignByCompany(
            @Parameter(description = "ID de la empresa", example = "1") @PathVariable Long companyId) {

        Optional<Sign> signOptional;
        try {
            signOptional = this.signService.findActiveSignByCompany(companyId);
        } catch (Exception e) {
            log.error("[SignController][getActiveSignByCompany][loadfiles] Error al intentar obtener la firma en la BD", e);
            throw new InternalServerErrorException("Error al intentar obtener la firma en la BD", e);
        }

        if (signOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Sign sign = signOptional.get();
        try {
            Resource fileResource = this.fileStorageService.loadFile(sign.getFileName(), sign.getFilePath());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sign.getFileName() + "\"")
                    .body(fileResource);
        } catch (MalformedURLException e) {
            log.error("[SignController][getActiveSignByCompany][loadfiles] Error al cargar la firma", e);
            throw new InternalServerErrorException("Error al cargar el archivo de firma: " + sign.getFileName());
        }
    }

    /** Retorna el archivo de firma activa de un usuario. */
    @Operation(summary = "Obtener firma activa de usuario",
        description = "Retorna el archivo de imagen de la firma activa asociada a un usuario. "
            + "Roles requeridos: todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Firma retornada exitosamente",
            content = @Content(mediaType = "application/octet-stream",
                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado: rol insuficiente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "No se encontró firma activa para el usuario",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error al cargar el archivo de firma",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PreAuthorize("@rolValidator.hasRol(authentication, 'todos')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Resource> getActiveSignByUser(
            @Parameter(description = "ID del usuario", example = "5") @PathVariable Long userId) {

        Optional<Sign> signOptional;
        try {
            signOptional = this.signService.findActiveSignByUser(userId);
        } catch (Exception e) {
            log.error("[SignController][getActiveSignByUser][loadfiles] Error al intentar obtener la firma en la BD", e);
            throw new InternalServerErrorException("Error al intentar obtener la firma en la BD", e);
        }

        if (signOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Sign sign = signOptional.get();
        try {
            Resource fileResource = this.fileStorageService.loadFile(sign.getFileName(), sign.getFilePath());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sign.getFileName() + "\"")
                    .body(fileResource);
        } catch (MalformedURLException e) {
            log.error("[SignController][getActiveSignByUser][loadfiles] Error al cargar la firma", e);
            throw new InternalServerErrorException("Error al cargar el archivo de firma: " + sign.getFileName());
        }
    }

}
