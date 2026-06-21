package com.loadfilesservice.loadfiles.service;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.loadfilesservice.loadfiles.entity.FileSigned;

public interface IFileSignedService {

	FileSigned save(FileSigned fileSigned);

	Optional<FileSigned> findById(Long id);

	List<FileSigned> findByCompanyAndState(Long companyId, Long state);

	FileSigned saveSignedFile(MultipartFile file, FileSigned fileSignedBase, String companyName);

}