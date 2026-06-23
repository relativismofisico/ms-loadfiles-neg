package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** DTO de respuesta para archivos de empresa. */
@Schema(description = "Información de un archivo de empresa")
@Data
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class CompanyFileDTOResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID del archivo", example = "1")
    private Long id;

    @Schema(description = "Nombre interno del archivo en el servidor", example = "1_20240101_contrato.pdf")
    private String fileName;

    @Schema(description = "Fecha y hora de carga del archivo")
    private Date loadTime;

    @Schema(description = "Dirección IP desde la que se realizó la carga")
    private String ipLoad;

    @Schema(description = "ID de la empresa propietaria del archivo", example = "1")
    private Long company;

    @Schema(description = "Tipo de archivo de empresa")
    private CompanyFileType companyFileType;

}
