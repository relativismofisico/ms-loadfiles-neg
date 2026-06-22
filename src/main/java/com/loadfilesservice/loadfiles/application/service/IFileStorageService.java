package com.loadfilesservice.loadfiles.application.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/** Contrato del servicio de almacenamiento de archivos. */
public interface IFileStorageService {

    /** Carga un archivo como recurso desde la ruta indicada. */
    Resource loadFile(String fileName, String path) throws MalformedURLException;

    /** Copia el multipart file en la ruta indicada y retorna el nombre generado. */
    String copyFile(MultipartFile file, String path) throws IOException;

    /** Elimina un archivo del sistema de ficheros. */
    boolean deleteFile(String fileName, String path);

    /** Crea la carpeta si no existe. */
    void createFolder(String folderPath);

    /** Retorna la ruta absoluta del archivo. */
    Path getPath(String fileName, String pathFile);

}
