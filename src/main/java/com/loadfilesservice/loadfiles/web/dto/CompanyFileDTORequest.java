package com.loadfilesservice.loadfiles.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/** DTO de solicitud para operaciones con archivos de empresa. */
@Schema(description = "Solicitud de carga de archivo de empresa")
@Data
@RequiredArgsConstructor
public class CompanyFileDTORequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Dirección IP desde la que se realiza la carga",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private String ipLoad;

    @Schema(description = "ID de la empresa", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long company;

    @Schema(description = "Tipo de archivo de empresa")
    private CompanyFileType companyFileType;

}
