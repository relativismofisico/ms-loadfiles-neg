package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** DTO de respuesta para archivos pendientes de firma. */
@Schema(description = "Información de un archivo pendiente de firma")
@Data
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class FileToSignDTOResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID del archivo", example = "1")
    private Long id;

    @Schema(description = "Nombre del archivo en el servidor", example = "20240101_contrato.pdf")
    private String fileName;

    @Schema(description = "Ruta del directorio donde se almacena el archivo", example = "files_to_sign")
    private String filePath;

    @Schema(description = "Tipo de archivo de empresa")
    private CompanyFileType companyFileType;

}
