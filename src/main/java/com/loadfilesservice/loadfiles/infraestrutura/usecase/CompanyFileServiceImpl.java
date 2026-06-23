package com.loadfilesservice.loadfiles.infraestrutura.usecase;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.application.ConstantVariables;
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException;
import com.loadfilesservice.loadfiles.application.service.ICompanyFileService;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ICompanyFileDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Implementación del servicio de archivos de empresa. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyFileServiceImpl implements ICompanyFileService {

    private final ICompanyFileDao companyFileDao;

    private final IFileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyFile> findById(Long id) {
        Optional<CompanyFile> result = companyFileDao.findById(id);
        if (result.isEmpty()) {
            log.error("[CompanyFileServiceImpl][findById][loadfiles] El archivo no se encuentra en la base de datos");
            throw new ResourceNotFoundException("El archivo no se pudo encontrar en la base de datos");
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyFile> findByCompanyAndCompanyFileType(Long companyId, CompanyFileType companyFileType) {
        return companyFileDao.findByCompanyAndCompanyFileType(companyId, companyFileType);
    }

    @Override
    @Transactional
    public CompanyFile save(CompanyFile companyFile) {
        return companyFileDao.save(companyFile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyFile> findByCompanyAndState(Long companyId, Long state) {
        List<CompanyFile> files = companyFileDao.findByCompanyAndState(companyId, state);
        files.forEach(f -> {
            if (f.getCompanyFileType() != null) {
                f.getCompanyFileType().getDescription();
            }
        });
        return files;
    }

    @Override
    @Transactional
    public CompanyFile replaceCompanyFile(MultipartFile file, CompanyFile companyFileBase) {

        List<CompanyFile> existing = companyFileDao.findByCompanyAndCompanyFileType(
                companyFileBase.getCompany(), companyFileBase.getCompanyFileType());

        CompanyFile oldCompanyFile = existing.stream()
                .filter(cf -> Long.valueOf(1L).equals(cf.getState()))
                .findFirst()
                .orElse(null);

        if (oldCompanyFile != null) {
            oldCompanyFile.setState(0L);

            try {
                companyFileDao.save(oldCompanyFile);
            } catch (Exception e) {
                log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] "
                        + "Error al intentar actualizar el registro del archivo en la base de datos: {}",
                        oldCompanyFile.getFileName(), e);
                throw new InternalServerErrorException(
                        "Error al intentar actualizar el registro del archivo en la base de datos: "
                                + oldCompanyFile.getFileName(), e);
            }

            try {
                fileStorageService.deleteFile(oldCompanyFile.getFileName(), ConstantVariables.PATH_UPLOADS);
            } catch (Exception e) {
                log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] "
                        + "Error al intentar borrar el archivo: {}",
                        oldCompanyFile.getFileName());
                throw new InternalServerErrorException(
                        "Error al intentar borrar el archivo: " + oldCompanyFile.getFileName());
            }
        }

        String newFileName;
        try {
            newFileName = fileStorageService.copyFile(file, ConstantVariables.PATH_UPLOADS);
        } catch (IOException e) {
            log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] "
                    + "Error al intentar guardar el archivo: {}",
                    file.getOriginalFilename(), e);
            throw new InternalServerErrorException(
                    "Error al intentar guardar el archivo: " + file.getOriginalFilename(), e);
        }

        companyFileBase.setFileName(newFileName);
        companyFileBase.setOriginalFileName(file.getOriginalFilename());
        companyFileBase.setFilePath(fileStorageService.getPath(newFileName, ConstantVariables.PATH_UPLOADS).toString());
        companyFileBase.setLoadTime(LocalDateTime.now());
        companyFileBase.setState(1L);

        try {
            return companyFileDao.save(companyFileBase);
        } catch (Exception e) {
            log.error("[CompanyFileServiceImpl][replaceCompanyFile][loadfiles] "
                    + "Error al intentar guardar el registro del archivo en la base de datos: {}",
                    newFileName, e);
            throw new InternalServerErrorException(
                    "Error al intentar guardar el registro del archivo en la base de datos: " + newFileName, e);
        }
    }

}
