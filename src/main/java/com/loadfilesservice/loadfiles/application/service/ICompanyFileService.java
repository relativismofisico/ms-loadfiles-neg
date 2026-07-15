package com.loadfilesservice.loadfiles.application.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import com.loadfilesservice.loadfiles.domain.DocumentReuploadToken;
import org.springframework.web.multipart.MultipartFile;

/** Contrato del servicio de archivos de empresa. */
public interface ICompanyFileService {

    /** Busca un archivo por su identificador. */
    Optional<CompanyFile> findById(Long id);

    /** Busca archivos por empresa y tipo de archivo. */
    List<CompanyFile> findByCompanyAndCompanyFileType(Long companyId, CompanyFileType companyFileType);

    /** Busca archivos por empresa y estado. */
    List<CompanyFile> findByCompanyAndState(Long companyId, Long state);

    /** Persiste un archivo de empresa. */
    CompanyFile save(CompanyFile companyFile);

    /** Reemplaza el archivo activo de la empresa por uno nuevo. */
    CompanyFile replaceCompanyFile(MultipartFile file, CompanyFile companyFileBase);

    /** Aprueba un documento cargado. */
    CompanyFile approve(Long id);

    /** Rechaza un documento cargado y notifica a la empresa por correo con el motivo. */
    CompanyFile reject(Long id, String rejectionReason, String companyEmail, String companyName);

    /**
     * Valida un token de recarga de documento rechazado (existe, no usado, no expirado).
     * Lanza ResourceNotFoundException si no existe, BadRequestException si ya fue usado o expiró.
     */
    DocumentReuploadToken validateReuploadToken(String token);

    /**
     * Canjea un token de recarga: sube el archivo en reemplazo del documento rechazado que
     * originó el token (mismo mecanismo que replaceCompanyFile) y marca el token como usado.
     */
    CompanyFile redeemReuploadToken(String token, MultipartFile file, String ipLoad);

}
