package com.loadfilesservice.loadfiles.application.service;

import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.Sign;
import org.springframework.web.multipart.MultipartFile;

/** Contrato del servicio de firmas digitales. */
public interface ISignService {

    /** Busca la firma activa de una empresa. */
    Optional<Sign> findActiveSignByCompany(Long company);

    /** Busca la firma activa de un usuario. */
    Optional<Sign> findActiveSignByUser(Long user);

    /** Persiste una firma. */
    Sign save(Sign sign);

    /** Reemplaza la firma activa y guarda la nueva en el sistema de ficheros. */
    Sign replaceSign(MultipartFile file, Long company, Long user, String ipLoad, String companyName);

}
