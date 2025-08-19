package com.loadfilesservice.loadfiles.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.entity.FileSigned;

public interface IFileSignedService {
	
	public FileSigned save(FileSigned fileSigned);
	
	public Optional<FileSigned> findById(Long id);
	
	public List<FileSigned> findByCompanyAndState(Long companyId, Long state);

}
