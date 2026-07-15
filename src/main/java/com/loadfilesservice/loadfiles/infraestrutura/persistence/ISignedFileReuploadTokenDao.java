package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.SignedFileReuploadToken;
import org.springframework.data.repository.CrudRepository;

/** Repositorio JPA para la entidad SignedFileReuploadToken. */
public interface ISignedFileReuploadTokenDao extends CrudRepository<SignedFileReuploadToken, Long> {

    /** Busca un token de recarga de documento firmado por su valor. */
    Optional<SignedFileReuploadToken> findByToken(String token);

}
