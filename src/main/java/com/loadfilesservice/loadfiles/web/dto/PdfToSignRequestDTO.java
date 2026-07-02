package com.loadfilesservice.loadfiles.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** DTO de solicitud para obtener un PDF pendiente de firma, prellenado con los datos de la empresa. */
@Schema(description = "Solicitud del PDF pendiente de firma junto con los datos para prellenar el formulario")
@Data
public class PdfToSignRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identificador del archivo pendiente de firma", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long id;

    @Schema(description = "Nombre de la empresa", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String companyName;

    @Schema(description = "NIT de la empresa", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String nit;

    @Schema(description = "Nombre completo del representante legal", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String representativeName;

}
