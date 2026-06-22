package com.loadfilesservice.loadfiles.application.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.FileToSign;

/** Contrato del servicio de archivos pendientes de firma. */
public interface IFileToSignService {

    /** Busca archivos por tipo de archivo y estado. */
    List<FileToSign> findByCompanyFileTypeAndState(Long companyFileType, Long state);

    /** Busca un archivo pendiente de firma por su identificador. */
    Optional<FileToSign> findById(Long id);

}
