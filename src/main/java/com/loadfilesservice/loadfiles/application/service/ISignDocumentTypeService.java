package com.loadfilesservice.loadfiles.application.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.SignDocumentType;

/** Contrato del servicio del catálogo parametrizable de documentos a firmar. */
public interface ISignDocumentTypeService {

    /** Retorna los tipos de documento a firmar activos. */
    List<SignDocumentType> findAllActive();

    /** Incluye inactivos, para la pantalla de administración. */
    List<SignDocumentType> findAll();

    /** Busca un tipo de documento a firmar por su identificador. */
    Optional<SignDocumentType> findById(Long id);

    /** Crea o actualiza un tipo de documento a firmar. */
    SignDocumentType save(SignDocumentType signDocumentType);

    /** Desactiva un tipo de documento a firmar por su identificador. */
    void deactivate(Long id);

}
