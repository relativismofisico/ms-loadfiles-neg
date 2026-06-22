package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;

import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/** DTO de solicitud para archivos firmados. */
@Data
@RequiredArgsConstructor
public class FileSignedDTORequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String originalFileName;

    private String ipLoad;

    private String companyName;

    private Long company;

    private Long user;

    private CompanyFileType companyFileType;

}
