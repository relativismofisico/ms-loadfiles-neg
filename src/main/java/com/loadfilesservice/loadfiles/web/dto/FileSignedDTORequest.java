package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;

import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/** DTO de solicitud para archivos firmados. */
@Schema(description = "Solicitud de carga de archivo firmado")
@Data
@RequiredArgsConstructor
public class FileSignedDTORequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Nombre original del archivo firmado", example = "contrato_empresa.pdf")
    private String originalFileName;

    @Schema(description = "Dirección IP desde la que se realizó la carga")
    private String ipLoad;

    @Schema(description = "Nombre identificador de la empresa", example = "EMPRESA_SA")
    private String companyName;

    @Schema(description = "ID de la empresa", example = "1")
    private Long company;

    @Schema(description = "ID del usuario que realiza la carga", example = "5")
    private Long user;

    @Schema(description = "Tipo de archivo de empresa")
    private CompanyFileType companyFileType;

}
