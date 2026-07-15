package com.loadfilesservice.loadfiles.application.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.FileToSign;
import org.springframework.web.multipart.MultipartFile;

/** Contrato del servicio de archivos pendientes de firma. */
public interface IFileToSignService {

    /** Busca un archivo pendiente de firma por su identificador. */
    Optional<FileToSign> findById(Long id);

    /** Plantillas vigentes de todos los tipos de documento a firmar activos. */
    List<FileToSign> findSignableTemplates();

    /** Lista todas las plantillas de firma (para la pantalla de administración), sin filtrar por estado. */
    List<FileToSign> findAllTemplates();

    /**
     * Sube una nueva plantilla PDF de firma para un tipo de documento a firmar, desactivando la
     * plantilla vigente anterior de ese mismo tipo (si existía).
     */
    FileToSign uploadTemplate(MultipartFile file, Long signDocumentTypeId, String ipLoad);

}
