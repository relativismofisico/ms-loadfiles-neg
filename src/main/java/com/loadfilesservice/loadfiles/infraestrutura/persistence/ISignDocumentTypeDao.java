package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.List;

import com.loadfilesservice.loadfiles.domain.SignDocumentType;
import org.springframework.data.repository.CrudRepository;

/** Repositorio JPA para la entidad SignDocumentType (catálogo de documentos a firmar). */
public interface ISignDocumentTypeDao extends CrudRepository<SignDocumentType, Long> {

    /** Retorna todos los tipos de documento a firmar activos. */
    List<SignDocumentType> findAllByActivoTrue();

}
