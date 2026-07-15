package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta con la información necesaria para mostrar la pantalla de recarga. */
@Schema(description = "Información del documento rechazado asociado a un token de recarga")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReuploadInfoDTOResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Nombre de la empresa dueña del documento", example = "Acme S.A.S.")
    private String companyName;

    @Schema(description = "Nombre del tipo de documento a recargar", example = "RUT")
    private String documentTypeName;

    @Schema(description = "Motivo por el que el documento fue rechazado")
    private String rejectionReason;

}
