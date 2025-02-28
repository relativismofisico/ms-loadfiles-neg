package com.loadfilesservice.loadfiles.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.entity.FileToSign;

public interface IFileToSignService {
	
	public List<FileToSign> findByCompanyFileTypeAndState(Long companyFileType, Long state);
	
	public Optional<FileToSign> findById(Long id);

}
