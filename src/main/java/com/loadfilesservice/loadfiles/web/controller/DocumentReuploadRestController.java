package com.loadfilesservice.loadfiles.web.controller;

import com.loadfilesservice.loadfiles.application.exception.ApiErrorResponse;
import com.loadfilesservice.loadfiles.application.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.DocumentReuploadToken;
import com.loadfilesservice.loadfiles.web.dto.CompanyFileDTOResponse;
import com.loadfilesservice.loadfiles.web.dto.Converter;
import com.loadfilesservice.loadfiles.web.dto.ReuploadInfoDTOResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador REST para la recarga de un documento rechazado a través del enlace de un solo
 * uso enviado por correo. Público (sin JWT): la empresa llega desde el correo sin haber
 * iniciado sesión, igual que el resto de endpoints anónimos de carga de documentos.
 */
@Tag(name = "Recarga de documento rechazado",
        description = "Consulta y canje del enlace de un solo uso enviado al rechazar un documento")
@CrossOrigin(origins = {"http://localhost:4300", "http://localhost:4500", "http://localhost:4600", "http://localhost:4700"})
@RestController
@RequestMapping("/api/companyfile/reupload")
@RequiredArgsConstructor
public class DocumentReuploadRestController {

    private final ICompanyFileService companyFileService;

    private final Converter converter;

    /** Consulta la información del enlace de recarga de un documento rechazado. */
    @Operation(summary = "Consultar el enlace de recarga de un documento rechazado",
        description = "Retorna la empresa, el tipo de documento y el motivo del rechazo, para mostrarlos "
            + "antes de pedir el nuevo archivo. Endpoint público.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Información del enlace retornada exitosamente"),
        @ApiResponse(responseCode = "400", description = "El enlace ya fue usado o expiró",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "El enlace no existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping(value = "/{token}", produces = "application/json")
    public ResponseEntity<ReuploadInfoDTOResponse> getReuploadInfo(
            @Parameter(description = "Token del enlace de recarga") @PathVariable String token) {
        DocumentReuploadToken reuploadToken = companyFileService.validateReuploadToken(token);
        CompanyFile rejectedFile = reuploadToken.getRejectedFile();

        String documentTypeName = "";
        if (rejectedFile.getCompanyFileType() != null) {
            documentTypeName = rejectedFile.getCompanyFileType().getFileTypeName();
        }

        ReuploadInfoDTOResponse response = ReuploadInfoDTOResponse.builder()
                .companyName(reuploadToken.getCompanyName())
                .documentTypeName(documentTypeName)
                .rejectionReason(rejectedFile.getRejectionReason())
                .build();

        return ResponseEntity.ok(response);
    }

    /** Canjea el enlace de recarga subiendo el nuevo documento. */
    @Operation(summary = "Canjear el enlace de recarga subiendo el nuevo documento",
        description = "Sube el archivo en reemplazo del documento rechazado que originó el enlace y marca "
            + "el enlace como usado. Endpoint público.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Documento cargado exitosamente"),
        @ApiResponse(responseCode = "400", description = "El enlace ya fue usado o expiró",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "El enlace no existe",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(value = "/{token}", produces = "application/json")
    public ResponseEntity<CompanyFileDTOResponse> redeemReuploadToken(
            @Parameter(description = "Token del enlace de recarga") @PathVariable String token,
            @Parameter(description = "Archivo PDF a subir") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Dirección IP desde la que se realiza la carga") @RequestParam("ipLoad") String ipLoad) {
        CompanyFile uploaded = companyFileService.redeemReuploadToken(token, file, ipLoad);

        return ResponseEntity.status(HttpStatus.OK).body(converter.companyFileToDTO(uploaded));
    }

}
