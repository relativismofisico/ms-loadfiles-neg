package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.List;
import java.util.Optional;

import com.loadfilesservice.loadfiles.domain.FileToSign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repositorio JPA para la entidad FileToSign. */
public interface IFileToSignDao extends JpaRepository<FileToSign, Long> {

    /**
     * Plantillas de firma vigentes: aquellas cuyo tipo de documento a firmar sigue activo,
     * en el estado indicado (1 = vigente).
     */
    @Query("select f from FileToSign f join fetch f.signDocumentType t "
            + "where t.activo = true and f.state = ?1")
    List<FileToSign> findSignableTemplatesByState(Long state);

    /** Plantilla vigente (state=1) de un tipo de documento a firmar específico, si existe. */
    @Query("select f from FileToSign f join fetch f.signDocumentType where f.signDocumentType.id = ?1 and f.state = 1")
    Optional<FileToSign> findActiveBySignDocumentType(Long signDocumentType);

}
