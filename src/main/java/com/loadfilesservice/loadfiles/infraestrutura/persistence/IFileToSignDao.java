package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.List;

import com.loadfilesservice.loadfiles.domain.FileToSign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repositorio JPA para la entidad FileToSign. */
public interface IFileToSignDao extends JpaRepository<FileToSign, Long> {

    /** Busca archivos pendientes de firma por tipo y estado cargando companyFileType en la misma consulta. */
    @Query("select f from FileToSign f join fetch f.companyFileType where f.companyFileType.id = ?1 and f.state = ?2")
    List<FileToSign> findByCompanyFileTypeAndState(Long companyFileType, Long state);

}
