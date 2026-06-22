package com.loadfilesservice.loadfiles.application.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.FileSigned;
import org.springframework.web.multipart.MultipartFile;

/** Contrato del servicio de archivos firmados. */
public interface IFileSignedService {

    /** Persiste un archivo firmado. */
    FileSigned save(FileSigned fileSigned);

    /** Busca un archivo firmado por su identificador. */
    Optional<FileSigned> findById(Long id);

    /** Busca archivos firmados por empresa y estado. */
    List<FileSigned> findByCompanyAndState(Long companyId, Long state);

    /** Guarda un archivo PDF firmado en el sistema de ficheros y en la BD. */
    FileSigned saveSignedFile(MultipartFile file, FileSigned fileSignedBase, String companyName);

}
