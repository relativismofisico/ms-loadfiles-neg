package com.loadfilesservice.loadfiles.dto;

import java.io.Serializable;

import com.loadfilesservice.loadfiles.entity.CompanyFileType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CompanyFileDTORequest implements Serializable{

	@NotEmpty
	private String ipLoad;

	@NotNull
	private Long company;
	
	private CompanyFileType companyFileType;
	
	private static final long serialVersionUID = 1L;
}
