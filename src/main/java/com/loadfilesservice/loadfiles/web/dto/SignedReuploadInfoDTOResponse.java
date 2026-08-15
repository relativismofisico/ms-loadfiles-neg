package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta con la información necesaria para mostrar la pantalla de recarga de un firmado. */
@Schema(description = "Información del documento firmado rechazado asociado a un token de recarga")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedReuploadInfoDTOResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identificador de la empresa dueña del documento", example = "78")
    private Long companyId;

    @Schema(description = "Nombre de la empresa dueña del documento", example = "Acme S.A.S.")
    private String companyName;

    @Schema(description = "Nombre del tipo de documento a firmar recargado", example = "Términos y uso de la plataforma")
    private String documentTypeName;

    @Schema(description = "Motivo por el que el documento firmado fue rechazado")
    private String rejectionReason;

}
