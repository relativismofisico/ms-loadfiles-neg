package com.loadfilesservice.loadfiles.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IFileStorageService {

	Resource loadFile(String fileName, String path) throws MalformedURLException;

	String copyFile(MultipartFile file, String path) throws IOException;

	boolean deleteFile(String fileName, String path);

	void createFolder(String folderPath);

	Path getPath(String fileName, String pathFile);

}