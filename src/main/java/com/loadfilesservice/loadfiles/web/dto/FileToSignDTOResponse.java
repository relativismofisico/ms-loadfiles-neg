package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import lombok.Data;

/** DTO de respuesta para archivos pendientes de firma. */
@Data
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class FileToSignDTOResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String fileName;

    private String filePath;

    private CompanyFileType companyFileType;

}
