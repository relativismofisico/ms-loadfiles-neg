package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;

import com.loadfilesservice.loadfiles.domain.CompanyFileType;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class FileSignedDTORequest implements Serializable {

    private String originalFileName;

    private String ipLoad;

    private String companyName;

    private Long company;

    private Long user;

    private CompanyFileType companyFileType;

    private static final long serialVersionUID = 1L;

}
