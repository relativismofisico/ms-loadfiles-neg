package com.loadfilesservice.loadfiles.application.service;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.CompanyFileType;

/** Contrato del servicio del catálogo parametrizable de tipos de documento. */
public interface ICompanyFileTypeService {

    /** Retorna los tipos de documento activos. */
    List<CompanyFileType> findAllActive();

    /** Incluye documentos inactivos, para la pantalla de administración. */
    List<CompanyFileType> findAll();

    /** Retorna los tipos de documento aplicables según los flags recibidos. */
    List<CompanyFileType> findApplicable(boolean factoring, boolean confirming, boolean fondeador);

    /** Busca un tipo de documento por su identificador. */
    Optional<CompanyFileType> findById(Long id);

    /** Crea o actualiza un tipo de documento. */
    CompanyFileType save(CompanyFileType companyFileType);

    /** Desactiva un tipo de documento por su identificador. */
    void deactivate(Long id);

}
