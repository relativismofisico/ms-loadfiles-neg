package com.loadfilesservice.loadfiles.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** DTO de solicitud para rechazar un documento cargado. */
@Schema(description = "Solicitud de rechazo de un documento de empresa")
@Data
public class CompanyFileRejectRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Motivo del rechazo, se incluye en el correo enviado a la empresa",
        requiredMode = Schema.RequiredMode.REQUIRED, example = "El documento está borroso, vuelve a cargarlo.")
    @NotBlank
    private String rejectionReason;

    @Schema(description = "Correo de la empresa dueña del documento, a donde se envía la notificación",
        requiredMode = Schema.RequiredMode.REQUIRED, example = "empresa@correo.com")
    @NotBlank
    private String companyEmail;

    @Schema(description = "Nombre de la empresa, usado en el cuerpo del correo",
        requiredMode = Schema.RequiredMode.REQUIRED, example = "Acme S.A.S.")
    @NotBlank
    private String companyName;

}
