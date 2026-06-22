package com.loadfilesservice.loadfiles.web.dto;

import java.io.Serializable;

import com.loadfilesservice.loadfiles.domain.CompanyFileType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CompanyFileDTORequest implements Serializable {

    @NotEmpty
    private String ipLoad;

    @NotNull
    private Long company;

    private CompanyFileType companyFileType;

    private static final long serialVersionUID = 1L;
}
