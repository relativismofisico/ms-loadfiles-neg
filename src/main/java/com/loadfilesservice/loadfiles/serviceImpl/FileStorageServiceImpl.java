package com.loadfilesservice.loadfiles.serviceImpl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.loadfilesservice.loadfiles.exceptions.InternalServerErrorException;
import com.loadfilesservice.loadfiles.service.IFileStorageService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileStorageServiceImpl implements IFileStorageService {

	@Override
	public Resource loadFile(String fileName, String path) throws MalformedURLException {
		Path pathFile = getPath(fileName, path);

		Resource resource = new UrlResource(pathFile.toUri());

		if (!resource.exists() && !resource.isReadable()) {
			log.error("El archivo {} no se encuentra.", fileName);
		}

		return resource;
	}

	@Override
	public String copyFile(MultipartFile file, String path) throws IOException {
		String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename().replace(" ", "");
		Path pathFile = getPath(fileName, path);

		Files.copy(file.getInputStream(), pathFile);

		return fileName;
	}

	@Override
	public boolean deleteFile(String fileName, String path) {
		if (fileName != null && fileName.length() > 0) {
			Path pathOldFile = getPath(fileName, path);
			File oldFile = pathOldFile.toFile();

			if (oldFile.exists() && oldFile.canRead()) {
				if (oldFile.delete()) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void createFolder(String folderPath) {
		File directory = new File(folderPath);

		if (!directory.exists()) {
			boolean created = directory.mkdirs();

			if (created) {
				log.info("[FileStorageServiceImpl][createFolder][loadfiles] La carpeta se creo con éxito {}", folderPath);
			} else {
				log.error("[FileStorageServiceImpl][createFolder][loadfiles] Error al intentar crear la carpeta {}", folderPath);
				throw new InternalServerErrorException("Error al intentar crear la carpeta " + folderPath);
			}
		} else {
			log.info("[FileStorageServiceImpl][createFolder][loadfiles] La carpeta ya existe");
		}
	}

	@Override
	public Path getPath(String fileName, String pathFile) {
		return Paths.get(pathFile).resolve(fileName).toAbsolutePath();
	}

}