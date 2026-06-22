package com.loadfilesservice.loadfiles.infraestrutura.usecase;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import com.loadfilesservice.loadfiles.application.ConstantVariables;
import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException;
import com.loadfilesservice.loadfiles.application.service.IFileStorageService;
import com.loadfilesservice.loadfiles.application.service.ISignService;
import com.loadfilesservice.loadfiles.domain.Sign;
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ISignDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Implementación del servicio de firmas digitales. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignServiceImpl implements ISignService {

    private final ISignDao signDao;

    private final IFileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Optional<Sign> findActiveSignByCompany(Long company) {
        return signDao.findActiveSignByCompany(company);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sign> findActiveSignByUser(Long user) {
        return signDao.findActiveSignByUser(user);
    }

    @Override
    @Transactional
    public Sign save(Sign sign) {
        return signDao.save(sign);
    }

    @Override
    @Transactional
    public Sign replaceSign(MultipartFile file, Long company, Long user, String ipLoad, String companyName) {

        String pathFileSign = ConstantVariables.SIGNATURES_FOLDER + "/" + companyName;
        fileStorageService.createFolder(pathFileSign);

        Optional<Sign> existingSign;
        try {
            existingSign = signDao.findActiveSignByCompany(company);
        } catch (Exception e) {
            log.error("[SignServiceImpl][replaceSign][loadfiles] Error al intentar consultar la firma en la base de datos", e);
            throw new InternalServerErrorException("Error al intentar consultar la firma en la base de datos", e);
        }

        if (existingSign.isPresent()) {
            Sign oldSign = existingSign.get();

            boolean deleted = fileStorageService.deleteFile(oldSign.getFileName(), pathFileSign);
            if (deleted) {
                log.info("Archivo antiguo eliminado: {}/{}", oldSign.getFilePath(), oldSign.getFileName());
            } else {
                log.warn("No se pudo eliminar el archivo antiguo: {}/{}", oldSign.getFilePath(), oldSign.getFileName());
            }

            oldSign.setState(0L);
            try {
                signDao.save(oldSign);
            } catch (Exception e) {
                log.error("[SignServiceImpl][replaceSign][loadfiles] Error al intentar actualizar la firma antigua en la BD", e);
                throw new InternalServerErrorException("Error al intentar actualizar la firma antigua en la BD", e);
            }
        }

        String fileName;
        try {
            fileName = fileStorageService.copyFile(file, pathFileSign);
        } catch (IOException e) {
            log.error("[SignServiceImpl][replaceSign][loadfiles] Error al intentar guardar el archivo de firma en la ruta", e);
            throw new InternalServerErrorException("Error al intentar guardar el archivo de firma en la ruta", e);
        }

        Sign sign = new Sign();
        sign.setFileName(fileName);
        sign.setFilePath(pathFileSign);
        sign.setIpLoad(ipLoad);
        sign.setCompany(company);
        sign.setUser(user);
        sign.setState(1L);
        sign.setLoadTime(LocalDateTime.now());

        try {
            return signDao.save(sign);
        } catch (Exception e) {
            log.error("[SignServiceImpl][replaceSign][loadfiles] Error al intentar guardar la nueva firma en la BD", e);
            throw new InternalServerErrorException("Error al intentar guardar la nueva firma en la BD", e);
        }
    }

}
