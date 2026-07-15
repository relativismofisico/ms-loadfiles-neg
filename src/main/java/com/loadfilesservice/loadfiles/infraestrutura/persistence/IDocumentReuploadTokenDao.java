package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.DocumentReuploadToken;
import org.springframework.data.repository.CrudRepository;

/** Repositorio JPA para la entidad DocumentReuploadToken. */
public interface IDocumentReuploadTokenDao extends CrudRepository<DocumentReuploadToken, Long> {

    /** Busca un token de recarga por su valor. */
    Optional<DocumentReuploadToken> findByToken(String token);

}
