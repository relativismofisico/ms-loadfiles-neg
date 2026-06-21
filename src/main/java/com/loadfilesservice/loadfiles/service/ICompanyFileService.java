package com.loadfilesservice.loadfiles.service;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.loadfilesservice.loadfiles.entity.CompanyFile;
import com.loadfilesservice.loadfiles.entity.CompanyFileType;

public interface ICompanyFileService {

	public Optional<CompanyFile> findById(Long id);

	public List<CompanyFile> findByCompanyAndCompanyFileType(Long companyId, CompanyFileType companyFileType);

	public List<CompanyFile> findByCompanyAndState(Long companyId, Long state);

	public CompanyFile save(CompanyFile companyFile);

	public CompanyFile replaceCompanyFile(MultipartFile file, CompanyFile companyFileBase);

}
