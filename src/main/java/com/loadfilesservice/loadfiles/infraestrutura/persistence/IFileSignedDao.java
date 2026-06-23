package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.List;

import com.loadfilesservice.loadfiles.domain.FileSigned;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repositorio JPA para la entidad FileSigned. */
public interface IFileSignedDao extends JpaRepository<FileSigned, Long> {

    /** Busca archivos firmados por empresa y estado cargando companyFileType en la misma consulta. */
    @Query("select f from FileSigned f join fetch f.companyFileType where f.company = ?1 and f.state = ?2")
    List<FileSigned> findByCompanyAndState(Long companyId, Long state);

}
