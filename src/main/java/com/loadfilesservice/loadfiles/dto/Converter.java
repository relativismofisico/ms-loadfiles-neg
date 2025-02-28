package com.loadfilesservice.loadfiles.dto;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.loadfilesservice.loadfiles.entity.CompanyFile;
import com.loadfilesservice.loadfiles.entity.FileToSign;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Component
@Data
@RequiredArgsConstructor
public class Converter {

	private final ModelMapper modelMapper = new ModelMapper();
	
	public CompanyFileDTOResponse companyFileToDTO(CompanyFile companyFile) {
		return modelMapper.map(companyFile, CompanyFileDTOResponse.class);
	}
	
	public CompanyFile companyFileDTOtoCompanyFile(CompanyFileDTORequest companyFileDTO) {
		return modelMapper.map(companyFileDTO, CompanyFile.class);
	}
	
	public FileToSignDTOResponse FileToSignToDTO(FileToSign fileToSign) {
		return modelMapper.map(fileToSign, FileToSignDTOResponse.class);
	}
	
}
