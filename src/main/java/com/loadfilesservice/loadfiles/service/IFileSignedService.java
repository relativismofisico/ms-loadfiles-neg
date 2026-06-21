package com.loadfilesservice.loadfiles.service;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.loadfilesservice.loadfiles.entity.FileSigned;

public interface IFileSignedService {

	public FileSigned save(FileSigned fileSigned);

	public Optional<FileSigned> findById(Long id);

	public List<FileSigned> findByCompanyAndState(Long companyId, Long state);

	public FileSigned saveSignedFile(MultipartFile file, FileSigned fileSignedBase, String companyName);

}
