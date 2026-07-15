package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.loadfilesservice.loadfiles.domain.SignDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** DTO de respuesta para archivos firmados. */
@Schema(description = "Información de un archivo firmado")
@Data
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class FileSignedDTOResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID del archivo", example = "1")
    private Long id;

    @Schema(description = "Nombre interno del archivo en el servidor", example = "1_20240101_contrato_firmado.pdf")
    private String fileName;

    @Schema(description = "Nombre original del archivo firmado", example = "contrato_empresa.pdf")
    private String originalFileName;

    @Schema(description = "Fecha y hora de carga del archivo")
    private Date loadTime;

    @Schema(description = "Dirección IP desde la que se realizó la carga")
    private String ipLoad;

    @Schema(description = "ID de la empresa propietaria del archivo", example = "1")
    private Long company;

    @Schema(description = "Tipo de documento a firmar")
    private SignDocumentType signDocumentType;

    @Schema(description = "Estado de revisión del documento", example = "PENDIENTE",
        allowableValues = {"PENDIENTE", "APROBADO", "RECHAZADO"})
    private String reviewStatus;

    @Schema(description = "Motivo del rechazo (solo presente si reviewStatus es RECHAZADO)")
    private String rejectionReason;

}
