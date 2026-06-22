package com.loadfilesservice.loadfiles.infraestrutura.usecase;

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

import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileStorageServiceImpl implements IFileStorageService {

    @Override
    public Resource loadFile(String fileName, String path) throws MalformedURLException {
        Path pathFile = getPath(fileName, path);
        Resource resource = new UrlResource(pathFile.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            log.error("[FileStorageServiceImpl][loadFile][loadfiles] El archivo {} no se encuentra o no es legible.", fileName);
            throw new InternalServerErrorException("El archivo no se encuentra o no es legible: " + fileName);
        }

        return resource;
    }

    @Override
    public String copyFile(MultipartFile file, String path) throws IOException {
        String rawName = file.getOriginalFilename();
        String originalName = rawName != null ? rawName.replace(" ", "") : "file";
        String fileName = UUID.randomUUID() + "_" + originalName;
        Path pathFile = getPath(fileName, path);

        Files.copy(file.getInputStream(), pathFile);

        return fileName;
    }

    @Override
    public boolean deleteFile(String fileName, String path) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        try {
            Path pathOldFile = getPath(fileName, path);
            return Files.deleteIfExists(pathOldFile);
        } catch (IOException e) {
            log.warn("[FileStorageServiceImpl][deleteFile][loadfiles] No se pudo eliminar el archivo: {}", fileName);
            return false;
        }
    }

    @Override
    public void createFolder(String folderPath) {
        try {
            Path dirPath = Paths.get(folderPath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                log.info("[FileStorageServiceImpl][createFolder][loadfiles] La carpeta se creo con éxito {}", folderPath);
            } else {
                log.info("[FileStorageServiceImpl][createFolder][loadfiles] La carpeta ya existe");
            }
        } catch (IOException e) {
            log.error("[FileStorageServiceImpl][createFolder][loadfiles] Error al intentar crear la carpeta {}", folderPath);
            throw new InternalServerErrorException("Error al intentar crear la carpeta " + folderPath);
        }
    }

    @Override
    public Path getPath(String fileName, String pathFile) {
        return Paths.get(pathFile).resolve(fileName).toAbsolutePath();
    }

}
