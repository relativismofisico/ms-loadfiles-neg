package com.loadfilesservice.loadfiles.infraestrutura.usecase;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.IFileToSignService;
import com.loadfilesservice.loadfiles.domain.FileToSign;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.IFileToSignDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación del servicio de archivos pendientes de firma. */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileToSignServiceImpl implements IFileToSignService {

    private final IFileToSignDao fileToSignDao;

    @Override
    @Transactional(readOnly = true)
    public List<FileToSign> findByCompanyFileTypeAndState(Long companyFileType, Long state) {
        List<FileToSign> files = fileToSignDao.findByCompanyFileTypeAndState(companyFileType, state);
        files.forEach(f -> {
            if (f.getCompanyFileType() != null) {
                f.getCompanyFileType().getDescription();
            }
        });
        return files;
    }

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

}
