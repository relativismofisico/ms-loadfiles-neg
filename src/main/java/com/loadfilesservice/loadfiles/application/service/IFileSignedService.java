package com.loadfilesservice.loadfiles.application.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.FileSigned;
import com.loadfilesservice.loadfiles.domain.SignedFileReuploadToken;
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

    /** Aprueba un documento firmado. */
    FileSigned approve(Long id);

    /** Rechaza un documento firmado y notifica a la empresa por correo con el motivo. */
    FileSigned reject(Long id, String rejectionReason, String companyEmail, String companyName);

    /**
     * Valida un token de recarga de documento firmado rechazado (existe, no usado, no expirado).
     * Lanza ResourceNotFoundException si no existe, BadRequestException si ya fue usado o expiró.
     */
    SignedFileReuploadToken validateReuploadToken(String token);

    /**
     * Canjea un token de recarga: sube el PDF firmado corregido en reemplazo del rechazado que
     * originó el token (la fila anterior pasa a state=0) y marca el token como usado.
     */
    FileSigned redeemReuploadToken(String token, MultipartFile file, String ipLoad);

}
