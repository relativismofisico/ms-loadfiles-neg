package com.loadfilesservice.loadfiles.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/** DTO de solicitud para operaciones con archivos de empresa. */
@Data
@RequiredArgsConstructor
public class CompanyFileDTORequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty
    private String ipLoad;

    @NotNull
    private Long company;

    private CompanyFileType companyFileType;

}
