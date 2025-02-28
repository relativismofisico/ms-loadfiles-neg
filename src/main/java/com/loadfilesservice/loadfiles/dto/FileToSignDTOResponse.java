package com.loadfilesservice.loadfiles.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.loadfilesservice.loadfiles.entity.CompanyFileType;

import lombok.Data;

@Data
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class FileToSignDTOResponse implements Serializable{
	 
	private Long id;
	
	private String fileName;
	
	private String filePath;
	
	private CompanyFileType companyFileType;
	 
	private static final long serialVersionUID = 1L;

}
