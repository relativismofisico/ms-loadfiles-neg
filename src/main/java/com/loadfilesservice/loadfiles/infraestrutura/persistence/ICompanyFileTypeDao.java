package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.CompanyFileType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/** Repositorio JPA para la entidad CompanyFileType (catálogo de documentos parametrizable). */
public interface ICompanyFileTypeDao extends CrudRepository<CompanyFileType, Long> {

    /** Busca un tipo de documento por su nombre, ignorando mayúsculas/minúsculas. */
    Optional<CompanyFileType> findByFileTypeNameIgnoreCase(String fileTypeName);

    /** Retorna todos los tipos de documento activos. */
    List<CompanyFileType> findAllByActivoTrue();

    /**
     * Documentos base más los específicos de las operaciones indicadas.
     * Si el listado de operaciones está vacío, retorna todos los activos.
     */
    @Query("select t from CompanyFileType t where t.activo = true and ("
            + "t.esBase = true"
            + " or (:incluirFactoring = true and t.aplicaFactoring = true)"
            + " or (:incluirConfirming = true and t.aplicaConfirming = true)"
            + " or (:incluirFondeador = true and t.aplicaFondeador = true))"
            + " order by t.id")
    List<CompanyFileType> findApplicable(
            boolean incluirFactoring, boolean incluirConfirming, boolean incluirFondeador);

}
