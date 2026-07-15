package com.loadfilesservice.loadfiles.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** DTO de solicitud para crear/editar un tipo de documento del catálogo parametrizable. */
@Schema(description = "Solicitud de creación/edición de un tipo de documento")
@Data
public class CompanyFileTypeDTORequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Descripción del documento", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private String description;

    @Schema(description = "Nombre del tipo de documento", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private String fileTypeName;

    @Schema(description = "Si se pide siempre, sin importar las operaciones de interés")
    private boolean esBase;

    @Schema(description = "Si aplica para empresas interesadas en factoring")
    private boolean aplicaFactoring;

    @Schema(description = "Si aplica para empresas interesadas en confirming")
    private boolean aplicaConfirming;

    @Schema(description = "Si aplica para empresas interesadas en ser fondeador")
    private boolean aplicaFondeador;

    @Schema(description = "Si el documento sigue vigente y se debe ofrecer")
    private boolean activo = true;

}
