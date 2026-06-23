package com.loadfilesservice.loadfiles.infraestrutura.persistence;

import java.util.List;

import com.loadfilesservice.loadfiles.domain.FileToSign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repositorio JPA para la entidad FileToSign. */
public interface IFileToSignDao extends JpaRepository<FileToSign, Long> {

    /** Busca archivos pendientes de firma por tipo y estado. */
    @Query(value = "select * from files_to_sign where file_type_ide = ?1 and state = ?2", nativeQuery = true)
    List<FileToSign> findByCompanyFileTypeAndState(Long companyFileType, Long state);

}
