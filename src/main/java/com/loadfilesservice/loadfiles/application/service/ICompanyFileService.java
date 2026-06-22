package com.loadfilesservice.loadfiles.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;

public interface ICompanyFileService {

    Optional<CompanyFile> findById(Long id);

    List<CompanyFile> findByCompanyAndCompanyFileType(Long companyId, CompanyFileType companyFileType);

    List<CompanyFile> findByCompanyAndState(Long companyId, Long state);

    CompanyFile save(CompanyFile companyFile);

    CompanyFile replaceCompanyFile(MultipartFile file, CompanyFile companyFileBase);

}
