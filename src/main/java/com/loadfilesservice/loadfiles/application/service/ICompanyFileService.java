package com.loadfilesservice.loadfiles.application.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
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

}
