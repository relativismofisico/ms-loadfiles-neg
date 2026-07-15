package com.loadfilesservice.loadfiles.infraestrutura.usecase;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.application.ConstantVariables;
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.application.service.IFileToSignService;
import com.loadfilesservice.loadfiles.application.service.ISignDocumentTypeService;
import com.loadfilesservice.loadfiles.domain.FileToSign;
import com.loadfilesservice.loadfiles.domain.SignDocumentType;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.IFileToSignDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Implementación del servicio de archivos pendientes de firma. */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileToSignServiceImpl implements IFileToSignService {

    private static final long ACTIVE_STATE = 1L;
    private static final long INACTIVE_STATE = 0L;

    private final IFileToSignDao fileToSignDao;

    private final ISignDocumentTypeService signDocumentTypeService;

    private final IFileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Optional<FileToSign> findById(Long id) {
        Optional<FileToSign> result = fileToSignDao.findById(id);
        if (result.isEmpty()) {
            log.error("[FileToSignServiceImpl][findById][loadfiles] El archivo no se encuentra en la base de datos");
            throw new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos");
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileToSign> findSignableTemplates() {
        return fileToSignDao.findSignableTemplatesByState(ACTIVE_STATE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileToSign> findAllTemplates() {
        return fileToSignDao.findAll();
    }

    @Override
    @Transactional
    public FileToSign uploadTemplate(MultipartFile file, Long signDocumentTypeId, String ipLoad) {
        SignDocumentType signDocumentType = signDocumentTypeService.findById(signDocumentTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("El tipo de documento a firmar no existe"));

        fileToSignDao.findActiveBySignDocumentType(signDocumentTypeId).ifPresent(previous -> {
            previous.setState(INACTIVE_STATE);
            fileToSignDao.save(previous);
        });

        fileStorageService.createFolder(ConstantVariables.FILES_REGISTRY_SIGN);

        String newFileName;
        try {
            newFileName = fileStorageService.copyFile(file, ConstantVariables.FILES_REGISTRY_SIGN);
        } catch (IOException e) {
            log.error("[FileToSignServiceImpl][uploadTemplate][loadfiles] "
                    + "Error al intentar guardar la plantilla: {}", file.getOriginalFilename(), e);
            throw new InternalServerErrorException(
                    "Error al intentar guardar la plantilla: " + file.getOriginalFilename(), e);
        }

        FileToSign template = new FileToSign();
        template.setFileName(newFileName);
        template.setFilePath(fileStorageService.getPath(newFileName, ConstantVariables.FILES_REGISTRY_SIGN).toString());
        template.setIpLoad(ipLoad);
        template.setLoadTime(java.time.LocalDateTime.now().toString());
        template.setState(ACTIVE_STATE);
        template.setSignDocumentType(signDocumentType);

        try {
            return fileToSignDao.save(template);
        } catch (Exception e) {
            log.error("[FileToSignServiceImpl][uploadTemplate][loadfiles] "
                    + "Error al intentar guardar el registro de la plantilla: {}", newFileName, e);
            throw new InternalServerErrorException(
                    "Error al intentar guardar el registro de la plantilla: " + newFileName, e);
        }
    }

}
