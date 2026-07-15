package com.loadfilesservice.loadfiles.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** DTO de solicitud para crear/editar un tipo de documento a firmar del catálogo. */
@Schema(description = "Solicitud de creación/edición de un tipo de documento a firmar")
@Data
public class SignDocumentTypeDTORequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Nombre del documento a firmar", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private String name;

    @Schema(description = "Descripción del documento a firmar", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private String description;

    @Schema(description = "Si el documento sigue vigente y se debe ofrecer para firma")
    private boolean activo = true;

}
