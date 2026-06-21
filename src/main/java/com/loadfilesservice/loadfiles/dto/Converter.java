package com.loadfilesservice.loadfiles.dto;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.loadfilesservice.loadfiles.entity.CompanyFile;
import com.loadfilesservice.loadfiles.entity.FileSigned;
import com.loadfilesservice.loadfiles.entity.FileToSign;

@Component
public class Converter {

	private final ModelMapper modelMapper = new ModelMapper();

	public CompanyFileDTOResponse companyFileToDTO(CompanyFile companyFile) {
		return modelMapper.map(companyFile, CompanyFileDTOResponse.class);
	}

	public CompanyFile companyFileDTOtoCompanyFile(CompanyFileDTORequest companyFileDTO) {
		return modelMapper.map(companyFileDTO, CompanyFile.class);
	}

	public FileToSignDTOResponse fileToSignToDTO(FileToSign fileToSign) {
		return modelMapper.map(fileToSign, FileToSignDTOResponse.class);
	}

	public FileSigned fileSignedDtoToFileSigned(FileSignedDTORequest fileSignedDTO) {
		return modelMapper.map(fileSignedDTO, FileSigned.class);
	}

}
