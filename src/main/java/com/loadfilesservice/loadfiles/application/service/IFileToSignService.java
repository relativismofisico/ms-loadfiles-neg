package com.loadfilesservice.loadfiles.application.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.FileToSign;

public interface IFileToSignService {

    List<FileToSign> findByCompanyFileTypeAndState(Long companyFileType, Long state);

    Optional<FileToSign> findById(Long id);

}
