package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;

import lombok.Data;

@Data
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class CompanyFileDTOResponse implements Serializable {

    private Long id;

    private String fileName;

    private Date loadTime;

    private String ipLoad;

    private Long company;

    private CompanyFileType companyFileType;

    private static final long serialVersionUID = 1L;

}
