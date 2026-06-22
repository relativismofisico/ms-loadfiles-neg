package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.List;

import com.loadfilesservice.loadfiles.domain.FileSigned;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repositorio JPA para la entidad FileSigned. */
public interface IFileSignedDao extends JpaRepository<FileSigned, Long> {

    /** Busca archivos firmados por empresa y estado. */
    @Query(value = "select * from db_companies.files_signed where company_ide = ?1 and state = ?2", nativeQuery = true)
    List<FileSigned> findByCompanyAndState(Long companyId, Long state);

}
