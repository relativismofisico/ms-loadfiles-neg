package com.loadfilesservice.loadfiles.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.loadfilesservice.loadfiles.entity.CompanyFile;
import com.loadfilesservice.loadfiles.entity.CompanyFileType;

public interface ICompanyFileService {

	public Optional<CompanyFile> findById(Long id);
	
	public List<CompanyFile> findByCompanyAndCompanyFileType(Long companyId, CompanyFileType companyFileType);
	
	public CompanyFile save(CompanyFile companyFile);
	
	public Resource loadFile(String fileName, String path) throws MalformedURLException;
	
	public String copyFile(MultipartFile file, String path) throws IOException;
	
	public boolean deleteFile(String fileName, String path);
	
	public void createFolder(String folderPath);
	
	public Path getPath(String fileName, String pathFile);
	
}
